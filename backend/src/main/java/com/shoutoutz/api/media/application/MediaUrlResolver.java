package com.shoutoutz.api.media.application;

import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.infrastructure.s3.MediaObjectKeyGenerator;
import com.shoutoutz.api.media.infrastructure.s3.MediaPublicUrlResolver;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 미디어 ID를 공개 URL로 바꾸는 애플리케이션 수준 해석기.
 * 목록 응답은 메타데이터를 ID 목록으로 한 번에 읽어 N+1 조회를 피한다.
 */
@Component
@RequiredArgsConstructor
public class MediaUrlResolver {

    private static final MediaVariant DEFAULT_VARIANT = MediaVariant.DISPLAY;
    private static final Pattern DESCRIPTION_MEDIA_REFERENCE =
            Pattern.compile("media://(\\d{1,18})(?!\\d)");

    private final MediaMetadataRepository mediaMetadataRepository;
    private final MediaObjectKeyGenerator mediaObjectKeyGenerator;
    private final MediaPublicUrlResolver mediaPublicUrlResolver;

    public URI resolve(Long mediaId) {
        return resolve(mediaId, DEFAULT_VARIANT);
    }

    public URI resolve(Long mediaId, MediaVariant variant) {
        if (mediaId == null) {
            return null;
        }
        return resolveAll(List.of(mediaId), variant).get(mediaId);
    }

    public Map<Long, URI> resolveAll(Collection<Long> mediaIds) {
        return resolveAll(mediaIds, DEFAULT_VARIANT);
    }

    public Map<Long, URI> resolveAll(Collection<Long> mediaIds, MediaVariant variant) {
        Set<Long> distinctIds = normalizeIds(mediaIds);
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        MediaVariant requestedVariant = variant == null ? DEFAULT_VARIANT : variant;

        return mediaMetadataRepository.findAllByIds(distinctIds).stream()
                .filter(this::isReady)
                .filter(metadata -> metadata.getId() != null)
                .collect(Collectors.toUnmodifiableMap(
                        MediaMetadata::getId,
                        metadata -> resolveReadyMetadata(metadata, requestedVariant)
                ));
    }

    public String replaceDescriptionReferences(String descriptionMd, Map<Long, URI> urls) {
        if (descriptionMd == null || descriptionMd.isEmpty()) {
            return descriptionMd;
        }

        var matcher = DESCRIPTION_MEDIA_REFERENCE.matcher(descriptionMd);
        StringBuffer replaced = new StringBuffer();
        while (matcher.find()) {
            Long mediaId = Long.parseLong(matcher.group(1));
            URI url = urls.get(mediaId);
            if (url == null) {
                continue;
            }
            matcher.appendReplacement(
                    replaced,
                    java.util.regex.Matcher.quoteReplacement(url.toString())
            );
        }
        matcher.appendTail(replaced);
        return replaced.toString();
    }

    /**
     * 프로젝트 상세 응답에 사용한 공개 URL을 저장용 media://{id} 참조로 되돌린다.
     * 수정 요청이 상세 조회 응답의 descriptionMd를 그대로 포함해도 원본 참조 형식을 유지한다.
     */
    public String replaceDescriptionUrlsWithReferences(String descriptionMd, Map<Long, URI> urls) {
        if (descriptionMd == null || descriptionMd.isEmpty() || urls == null || urls.isEmpty()) {
            return descriptionMd;
        }

        String normalized = descriptionMd;
        for (Map.Entry<Long, URI> entry : urls.entrySet()) {
            Long mediaId = entry.getKey();
            URI url = entry.getValue();
            if (mediaId == null || url == null) {
                continue;
            }
            normalized = normalized.replace(url.toString(), "media://" + mediaId);
        }
        return normalized;
    }

    private URI resolveReadyMetadata(MediaMetadata metadata, MediaVariant variant) {
        String objectKey = mediaObjectKeyGenerator.generateVariant(
                metadata.getS3Key(),
                variant
        );
        return mediaPublicUrlResolver.resolve(objectKey);
    }

    private boolean isReady(MediaMetadata metadata) {
        return metadata != null && metadata.getStatus() == MediaStatus.READY;
    }

    private Set<Long> normalizeIds(Collection<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return Set.of();
        }
        return mediaIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}

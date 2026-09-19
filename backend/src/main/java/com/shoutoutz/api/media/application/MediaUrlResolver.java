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
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
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
    private static final Pattern DESCRIPTION_IMAGE_SOURCE = Pattern.compile(
            "!\\[[^\\]]*]\\(\\s*<?([^\\s)>]+)"
    );
    private static final Pattern DESCRIPTION_PUBLIC_IMAGE_URL = Pattern.compile(
            "!\\[[^\\]]*]\\(\\s*<?(https?://[^\\s)>]+)",
            Pattern.CASE_INSENSITIVE
    );

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
     * URL의 query와 fragment는 무시하고 scheme, host, port, path가 같은 공개 리소스만 변환한다.
     */
    public String replaceDescriptionUrlsWithReferences(String descriptionMd, Map<Long, URI> urls) {
        if (descriptionMd == null || descriptionMd.isEmpty() || urls == null || urls.isEmpty()) {
            return descriptionMd;
        }

        Matcher matcher = DESCRIPTION_PUBLIC_IMAGE_URL.matcher(descriptionMd);
        StringBuffer replaced = new StringBuffer();
        while (matcher.find()) {
            Long mediaId = findMatchingMediaId(matcher.group(1), urls);
            if (mediaId == null) {
                matcher.appendReplacement(
                        replaced,
                        Matcher.quoteReplacement(matcher.group())
                );
                continue;
            }

            String match = matcher.group();
            int urlStart = matcher.start(1) - matcher.start();
            int urlEnd = matcher.end(1) - matcher.start();
            String replacement = match.substring(0, urlStart)
                    + "media://" + mediaId
                    + match.substring(urlEnd);
            matcher.appendReplacement(replaced, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(replaced);
        return replaced.toString();
    }

    /**
     * 프로젝트 본문에 저장용 참조로 변환되지 않은 이미지 소스가 있는지 확인한다.
     * 프로젝트 수정 시 매칭되지 않은 외부 URL이나 지원하지 않는 이미지 소스가 미디어 검증을 우회하지 않도록 사용한다.
     */
    public boolean containsUnsupportedDescriptionImageReference(String descriptionMd) {
        if (descriptionMd == null) {
            return false;
        }
        Matcher matcher = DESCRIPTION_IMAGE_SOURCE.matcher(descriptionMd);
        while (matcher.find()) {
            if (!DESCRIPTION_MEDIA_REFERENCE.matcher(matcher.group(1)).matches()) {
                return true;
            }
        }
        return false;
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

    private Long findMatchingMediaId(String rawUrl, Map<Long, URI> urls) {
        URI requestedUrl = parseUri(rawUrl);
        if (requestedUrl == null) {
            return null;
        }
        return urls.entrySet().stream()
                .filter(entry -> entry.getKey() != null && samePublicResource(requestedUrl, entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private URI parseUri(String rawUrl) {
        try {
            return URI.create(rawUrl);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean samePublicResource(URI requestedUrl, URI expectedUrl) {
        if (expectedUrl == null
                || requestedUrl.getHost() == null
                || expectedUrl.getHost() == null
                || requestedUrl.getUserInfo() != null
                || expectedUrl.getUserInfo() != null) {
            return false;
        }
        return requestedUrl.getScheme().equalsIgnoreCase(expectedUrl.getScheme())
                && requestedUrl.getHost().equalsIgnoreCase(expectedUrl.getHost())
                && effectivePort(requestedUrl) == effectivePort(expectedUrl)
                && Objects.equals(requestedUrl.getPath(), expectedUrl.getPath());
    }

    private int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
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

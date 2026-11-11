package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.media.application.MediaUrlResolver;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCardImages;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 자료 카드들의 팀 로고와 블로그 파비콘 주소를 구한다. 카드가 몇 장이든 로고 조회 한 번, 파비콘 조회 한 번이다.
 * 로고는 카드 안 작은 아이콘이라 미디어의 가장 작은 크기(THUMBNAIL, 긴 변 320px)를 쓴다.
 */
@Component
@RequiredArgsConstructor
public class ResourceCardImageResolver {

    private static final MediaVariant LOGO_VARIANT = MediaVariant.THUMBNAIL;

    private final MediaUrlResolver mediaUrlResolver;
    private final ResourceFaviconQueryRepository resourceFaviconQueryRepository;

    public ResourceCardImages resolve(Collection<ResourceCard> cards) {
        Set<Long> logoMediaIds = cards.stream()
                .map(ResourceCard::team)
                .filter(Objects::nonNull)
                .map(ResourceCard.Team::logoMediaId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        List<Long> faviconResourceIds = cards.stream()
                .filter(ResourceCardImages::needsCollectedFavicon)
                .map(ResourceCard::resourceId)
                .distinct()
                .toList();
        if (logoMediaIds.isEmpty() && faviconResourceIds.isEmpty()) {
            return ResourceCardImages.NONE;
        }
        return new ResourceCardImages(
                logoMediaIds.isEmpty() ? Map.of() : mediaUrlResolver.resolveAll(logoMediaIds, LOGO_VARIANT),
                resourceFaviconQueryRepository.findFaviconUrls(faviconResourceIds)
        );
    }
}

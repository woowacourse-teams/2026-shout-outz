package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.media.application.MediaUrlResolver;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCardLogoUrls;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 자료 카드들의 팀 로고 미디어 ID를 이미지 주소로 바꾼다. 카드가 몇 장이든 미디어 조회는 한 번이다.
 * 로고는 카드 안 작은 아이콘이라 미디어의 가장 작은 크기(THUMBNAIL, 긴 변 320px)를 쓴다.
 */
@Component
@RequiredArgsConstructor
public class ResourceCardLogoResolver {

    private static final MediaVariant LOGO_VARIANT = MediaVariant.THUMBNAIL;

    private final MediaUrlResolver mediaUrlResolver;

    public ResourceCardLogoUrls resolve(Collection<ResourceCard> cards) {
        Set<Long> logoMediaIds = cards.stream()
                .map(ResourceCard::team)
                .filter(Objects::nonNull)
                .map(ResourceCard.Team::logoMediaId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        if (logoMediaIds.isEmpty()) {
            return ResourceCardLogoUrls.NONE;
        }
        return new ResourceCardLogoUrls(mediaUrlResolver.resolveAll(logoMediaIds, LOGO_VARIANT));
    }
}

package com.shoutoutz.api.homebanner.presentation.dto.response;

import com.shoutoutz.api.homebanner.domain.BannerDestinationType;
import com.shoutoutz.api.homebanner.domain.BannerLinkType;
import com.shoutoutz.api.homebanner.domain.BannerTargetType;
import com.shoutoutz.api.homebanner.domain.HomeBanner;
import java.net.URI;

public record HomeBannerResponse(
        long bannerId,
        URI imageUrl,
        BannerDestinationType destinationType,
        BannerTargetType targetType,
        Long targetId,
        BannerLinkType linkType,
        String linkUrl
) {

    public static HomeBannerResponse from(HomeBanner banner, URI imageUrl) {
        return new HomeBannerResponse(
                banner.getId(),
                imageUrl,
                banner.getDestinationType(),
                banner.getTargetType(),
                banner.getTargetId(),
                banner.getLinkType(),
                banner.getLinkUrl()
        );
    }
}

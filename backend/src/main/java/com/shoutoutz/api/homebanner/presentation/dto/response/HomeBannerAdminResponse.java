package com.shoutoutz.api.homebanner.presentation.dto.response;

import com.shoutoutz.api.homebanner.domain.BannerDestinationType;
import com.shoutoutz.api.homebanner.domain.BannerLinkType;
import com.shoutoutz.api.homebanner.domain.BannerTargetType;
import com.shoutoutz.api.homebanner.domain.HomeBanner;
import java.net.URI;
import java.time.Instant;

public record HomeBannerAdminResponse(
        long bannerId,
        long mediaId,
        URI imageUrl,
        BannerDestinationType destinationType,
        BannerTargetType targetType,
        Long targetId,
        BannerLinkType linkType,
        String linkUrl,
        int displayOrder,
        boolean active,
        long createdBy,
        Instant createdAt,
        Instant updatedAt
) {

    public static HomeBannerAdminResponse from(HomeBanner banner, URI imageUrl) {
        return new HomeBannerAdminResponse(
                banner.getId(),
                banner.getMediaId(),
                imageUrl,
                banner.getDestinationType(),
                banner.getTargetType(),
                banner.getTargetId(),
                banner.getLinkType(),
                banner.getLinkUrl(),
                banner.getDisplayOrder(),
                banner.isActive(),
                banner.getCreatedBy(),
                banner.getCreatedAt(),
                banner.getUpdatedAt()
        );
    }
}

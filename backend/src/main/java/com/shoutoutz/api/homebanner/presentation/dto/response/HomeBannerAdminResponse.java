package com.shoutoutz.api.homebanner.presentation.dto.response;

import java.net.URI;
import java.time.Instant;

public record HomeBannerAdminResponse(
        long bannerId,
        long mediaId,
        URI imageUrl,
        String destinationType,
        String targetType,
        Long targetId,
        String linkType,
        String linkUrl,
        int displayOrder,
        boolean active,
        long createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}

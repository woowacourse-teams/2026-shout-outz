package com.shoutoutz.api.homebanner.presentation.dto.response;

import java.net.URI;

public record HomeBannerResponse(
        long bannerId,
        long mediaId,
        URI imageUrl,
        String destinationType,
        String targetType,
        Long targetId,
        String linkType,
        String linkUrl
) {
}

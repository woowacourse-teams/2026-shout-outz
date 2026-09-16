package com.shoutoutz.api.homebanner.presentation.dto.request;

import com.shoutoutz.api.homebanner.domain.BannerDestinationType;
import com.shoutoutz.api.homebanner.domain.BannerLinkType;
import com.shoutoutz.api.homebanner.domain.BannerTargetType;
import com.shoutoutz.api.homebanner.domain.HomeBanner;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record HomeBannerUpsertRequest(
        @NotNull @Positive Long mediaId,
        @NotNull BannerDestinationType destinationType,
        BannerTargetType targetType,
        @Positive Long targetId,
        BannerLinkType linkType,
        String linkUrl,
        @NotNull @PositiveOrZero Integer displayOrder,
        @NotNull Boolean active
) {

    @AssertTrue(message = "이동 방식에 맞는 대상 또는 URL 정보가 필요합니다.")
    public boolean isDestinationValid() {
        return HomeBanner.isValidDestination(
                destinationType,
                targetType,
                targetId,
                linkType,
                linkUrl
        );
    }

    public HomeBanner toHomeBanner(long createdBy) {
        return HomeBanner.create(
                mediaId,
                destinationType,
                targetType,
                targetId,
                linkType,
                linkUrl,
                displayOrder,
                active,
                createdBy
        );
    }
}

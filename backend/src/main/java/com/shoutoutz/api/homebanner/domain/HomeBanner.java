package com.shoutoutz.api.homebanner.domain;

import static com.shoutoutz.api.homebanner.domain.HomeBannerErrorCode.HOME_BANNER_INVALID_STATE;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;

@Getter
public final class HomeBanner {

    private static final int MAX_LINK_URL_LENGTH = 2_048;

    private final Long id;
    private final long mediaId;
    private final BannerDestinationType destinationType;
    private final BannerTargetType targetType;
    private final Long targetId;
    private final BannerLinkType linkType;
    private final String linkUrl;
    private final int displayOrder;
    private final boolean active;
    private final long createdBy;
    private final Instant createdAt;
    private final Instant updatedAt;

    private HomeBanner(
            Long id,
            long mediaId,
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
        this.id = validateNullableId(id);
        this.mediaId = validatePositive(mediaId);
        this.destinationType = Objects.requireNonNull(destinationType, "이동 방식은 필수입니다.");
        this.targetType = targetType;
        this.targetId = targetId;
        this.linkType = linkType;
        this.linkUrl = normalizeLinkUrl(linkUrl);
        this.displayOrder = validateDisplayOrder(displayOrder);
        this.active = active;
        this.createdBy = validatePositive(createdBy);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        validateDestination();
    }

    public static HomeBanner create(
            long mediaId,
            BannerDestinationType destinationType,
            BannerTargetType targetType,
            Long targetId,
            BannerLinkType linkType,
            String linkUrl,
            int displayOrder,
            boolean active,
            long createdBy
    ) {
        return new HomeBanner(
                null,
                mediaId,
                destinationType,
                targetType,
                targetId,
                linkType,
                linkUrl,
                displayOrder,
                active,
                createdBy,
                null,
                null
        );
    }

    public static HomeBanner reconstitute(
            long id,
            long mediaId,
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
        Objects.requireNonNull(createdAt, "생성 시각은 필수입니다.");
        Objects.requireNonNull(updatedAt, "수정 시각은 필수입니다.");
        return new HomeBanner(
                id,
                mediaId,
                destinationType,
                targetType,
                targetId,
                linkType,
                linkUrl,
                displayOrder,
                active,
                createdBy,
                createdAt,
                updatedAt
        );
    }

    public HomeBanner update(
            long mediaId,
            BannerDestinationType destinationType,
            BannerTargetType targetType,
            Long targetId,
            BannerLinkType linkType,
            String linkUrl,
            int displayOrder,
            boolean active
    ) {
        return new HomeBanner(
                id,
                mediaId,
                destinationType,
                targetType,
                targetId,
                linkType,
                linkUrl,
                displayOrder,
                active,
                createdBy,
                createdAt,
                updatedAt
        );
    }

    private void validateDestination() {
        if (!isValidDestination(
                destinationType,
                targetType,
                targetId,
                linkType,
                linkUrl
        )) {
            throw invalidState();
        }
    }

    public static boolean isValidDestination(
            BannerDestinationType destinationType,
            BannerTargetType targetType,
            Long targetId,
            BannerLinkType linkType,
            String linkUrl
    ) {
        if (destinationType == null) {
            return false;
        }
        if (destinationType == BannerDestinationType.TARGET) {
            return targetType != null
                    && targetId != null
                    && targetId > 0
                    && linkType == null
                    && linkUrl == null;
        }
        if (targetType != null || targetId != null || linkType == null || linkUrl == null) {
            return false;
        }

        String normalized = linkUrl.strip();
        if (normalized.isEmpty() || normalized.length() > MAX_LINK_URL_LENGTH) {
            return false;
        }

        if (linkType == BannerLinkType.INTERNAL_PATH) {
            return isValidInternalPath(normalized);
        }
        return isValidExternalUrl(normalized);
    }

    private static boolean isValidInternalPath(String value) {
        if (!value.startsWith("/") || value.startsWith("//")) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            return !uri.isAbsolute() && uri.getHost() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean isValidExternalUrl(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static Long validateNullableId(Long value) {
        if (value != null && value <= 0) {
            throw invalidState();
        }
        return value;
    }

    private static long validatePositive(long value) {
        if (value <= 0) {
            throw invalidState();
        }
        return value;
    }

    private static int validateDisplayOrder(int value) {
        if (value < 0) {
            throw invalidState();
        }
        return value;
    }

    private static String normalizeLinkUrl(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > MAX_LINK_URL_LENGTH) {
            throw invalidState();
        }
        return normalized;
    }

    private static DomainValidationException invalidState() {
        return new DomainValidationException(HOME_BANNER_INVALID_STATE);
    }
}

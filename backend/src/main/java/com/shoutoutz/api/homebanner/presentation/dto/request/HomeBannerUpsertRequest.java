package com.shoutoutz.api.homebanner.presentation.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.net.URI;

public record HomeBannerUpsertRequest(
        @NotNull @Positive Long mediaId,
        @NotBlank @Pattern(regexp = "TARGET|URL") String destinationType,
        @Pattern(regexp = "NEWS|PROJECT|FEED") String targetType,
        @Positive Long targetId,
        @Pattern(regexp = "INTERNAL_PATH|EXTERNAL_URL") String linkType,
        String linkUrl,
        @NotNull @PositiveOrZero Integer displayOrder,
        @NotNull Boolean active
) {

    @AssertTrue(message = "이동 방식에 맞는 대상 또는 URL 정보가 필요합니다.")
    public boolean isDestinationValid() {
        if ("TARGET".equals(destinationType)) {
            return targetType != null
                    && targetId != null
                    && linkType == null
                    && linkUrl == null;
        }
        if (!"URL".equals(destinationType)
                || targetType != null
                || targetId != null
                || linkType == null
                || linkUrl == null) {
            return false;
        }

        String normalized = linkUrl.strip();
        if (normalized.isEmpty() || normalized.length() > 2_048) {
            return false;
        }
        return switch (linkType) {
            case "INTERNAL_PATH" -> isValidInternalPath(normalized);
            case "EXTERNAL_URL" -> isValidExternalUrl(normalized);
            default -> false;
        };
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
}

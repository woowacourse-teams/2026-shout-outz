package com.shoutoutz.api.media.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 공개 미디어를 제공하는 CloudFront 배포 설정.
 */
@Validated
@ConfigurationProperties(prefix = "aws.cloudfront")
public record CloudFrontProperties(
        @NotBlank String publicBaseUrl
) {

    public CloudFrontProperties {
        String normalized = publicBaseUrl == null ? null : publicBaseUrl.strip();
        validatePublicBaseUrl(normalized);
        publicBaseUrl = normalized.endsWith("/") ? normalized : normalized + "/";
    }

    public URI publicBaseUri() {
        return URI.create(publicBaseUrl);
    }

    private static void validatePublicBaseUrl(String value) {
        if (value == null || value.isBlank() || value.contains(" ")) {
            throw new IllegalArgumentException(
                    "CloudFront 공개 URL은 공백이 없는 절대 URL이어야 합니다."
            );
        }

        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "CloudFront 공개 URL은 공백이 없는 절대 URL이어야 합니다.",
                    exception
            );
        }

        String scheme = uri.getScheme();
        if (scheme == null
                || !(scheme.toLowerCase(Locale.ROOT).equals("http")
                || scheme.toLowerCase(Locale.ROOT).equals("https"))
                || uri.getHost() == null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            throw new IllegalArgumentException(
                    "CloudFront 공개 URL은 공백이 없는 절대 URL이어야 합니다."
            );
        }
    }
}

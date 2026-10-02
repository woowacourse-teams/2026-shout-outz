package com.shoutoutz.api.media.infrastructure.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * S3 연결에 필요한 백엔드 설정
 * 비밀 값은 포함하지 않으며, 자격 증명은 AWS SDK 기본 자격 증명 체인이 조회한다.
 *
 * @author josangjun
 */
@Validated
@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(
        @NotBlank String bucket,
        @NotBlank String region,
        @NotBlank
        @Pattern(regexp = ".*/", message = "S3 key prefix는 /로 끝나야 합니다.")
        String keyPrefix,
        @Min(1) @Max(604_800) long presignedUrlExpirationSeconds
) {

    public S3Properties {
        validateKeyPrefix(keyPrefix);
    }

    public Duration presignedUrlExpiration() {
        return Duration.ofSeconds(presignedUrlExpirationSeconds);
    }

    private static void validateKeyPrefix(String keyPrefix) {
        if (keyPrefix == null
                || keyPrefix.isBlank()
                || !keyPrefix.equals(keyPrefix.strip())
                || keyPrefix.length() > 1_024
                || keyPrefix.startsWith("/")
                || !keyPrefix.endsWith("/")
                || keyPrefix.contains("//")
                || keyPrefix.contains("\\")
                || keyPrefix.contains("\u0000")
                || keyPrefix.startsWith("s3://")
                || hasRelativePathSegment(keyPrefix)) {
            throw new IllegalArgumentException(
                    "S3 key prefix는 앞에 /가 없고 /로 끝나는 유효한 prefix여야 합니다."
            );
        }
    }

    private static boolean hasRelativePathSegment(String keyPrefix) {
        String[] segments = keyPrefix.split("/");
        for (String segment : segments) {
            if (segment.equals(".") || segment.equals("..")) {
                return true;
            }
        }
        return false;
    }
}

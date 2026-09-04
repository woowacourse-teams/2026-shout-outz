package com.shoutoutz.api.media.infrastructure.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
        @Min(1) @Max(604_800) long presignedUrlExpirationSeconds
) {

    public Duration presignedUrlExpiration() {
        return Duration.ofSeconds(presignedUrlExpirationSeconds);
    }
}

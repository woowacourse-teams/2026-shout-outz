package com.shoutoutz.api.auth.infrastructure.oauth.github;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github.oauth")
public record GitHubOAuthProperties(
        String clientId,
        String clientSecret,
        URI redirectUri,
        Duration connectTimeout,
        Duration readTimeout
) {

    public GitHubOAuthProperties {
        validatePositive(connectTimeout, "GitHub OAuth 연결 타임아웃");
        validatePositive(readTimeout, "GitHub OAuth 응답 타임아웃");
    }

    private static void validatePositive(Duration timeout, String name) {
        Objects.requireNonNull(timeout, name + "이 없습니다.");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(name + "은 양수여야 합니다.");
        }
    }
}

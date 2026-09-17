package com.shoutoutz.api.visitor;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 방문자 식별 쿠키와 식별값 해시에 필요한 설정
 * 쿠키 원래 값은 저장하지 않고, hashSecret 으로 해시한 값만 저장한다.
 */
@ConfigurationProperties(prefix = "visitor")
public record VisitorProperties(
        String cookieName,
        Duration cookieMaxAge,
        boolean cookieSecure,
        String cookieSameSite,
        String hashSecret
) {

    private static final int MIN_HASH_SECRET_LENGTH = 32;

    public VisitorProperties {
        validateNotBlank(cookieName, "방문자 쿠키 이름");
        validatePositive(cookieMaxAge);
        validateNotBlank(cookieSameSite, "방문자 쿠키 SameSite 값");
        validateHashSecret(hashSecret);
    }

    private static void validateNotBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "이 없습니다.");
        }
    }

    private static void validatePositive(Duration maxAge) {
        Objects.requireNonNull(maxAge, "방문자 쿠키 만료 기간이 없습니다.");
        if (maxAge.isZero() || maxAge.isNegative()) {
            throw new IllegalArgumentException("방문자 쿠키 만료 기간은 양수여야 합니다.");
        }
    }

    /**
     * HMAC-SHA256 키로 쓰므로, 추측하기 어렵도록 32자 이상을 요구한다.
     */
    private static void validateHashSecret(String hashSecret) {
        if (hashSecret == null || hashSecret.length() < MIN_HASH_SECRET_LENGTH) {
            throw new IllegalArgumentException(
                    "방문자 해시 비밀키는 " + MIN_HASH_SECRET_LENGTH + "자 이상이어야 합니다."
            );
        }
    }
}

package com.shoutoutz.api.visitor;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VisitorPropertiesTest {

    private static final String VALID_SECRET = "a".repeat(32);

    @Test
    @DisplayName("올바른 설정값이면 생성된다")
    void createsWithValidValues() {
        assertThatCode(() -> properties(Duration.ofDays(365), VALID_SECRET))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("방문자 쿠키 만료 기간은 양수여야 한다")
    void rejectsNonPositiveMaxAge() {
        assertThatThrownBy(() -> properties(Duration.ZERO, VALID_SECRET))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("방문자 해시 비밀키는 32자 이상이어야 한다")
    void rejectsShortHashSecret() {
        assertThatThrownBy(() -> properties(Duration.ofDays(365), "a".repeat(31)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("방문자 해시 비밀키는 32자 이상이어야 합니다.");
    }

    private VisitorProperties properties(Duration maxAge, String hashSecret) {
        return new VisitorProperties("VISITOR_ID", maxAge, true, "Lax", hashSecret);
    }
}

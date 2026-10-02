package com.shoutoutz.api.visitor.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.visitor.VisitorProperties;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VisitorKeyHasherTest {

    private static final String SECRET = "a".repeat(32);
    private static final String VISITOR_ID = "3f2a1b4c-5d6e-4f70-8a9b-0c1d2e3f4a5b";

    private final VisitorKeyHasher hasher = hasher(SECRET);

    @Test
    @DisplayName("비밀키로 HMAC-SHA256 을 계산해 소문자 hex 로 반환한다")
    void hashesWithHmacSha256() {
        assertThat(hasher.hash(VISITOR_ID))
                .isEqualTo("bbb8734dc891e004bb2560ef699fe4fd5fd34ec79ce18a0cbdcc6c41875d1e75");
    }

    @Test
    @DisplayName("같은 방문자 식별값은 항상 같은 해시가 된다")
    void returnsSameHashForSameVisitor() {
        assertThat(hasher.hash(VISITOR_ID)).isEqualTo(hasher.hash(VISITOR_ID));
    }

    @Test
    @DisplayName("다른 방문자 식별값은 다른 해시가 된다")
    void returnsDifferentHashForDifferentVisitor() {
        String otherVisitorId = "7c8d9e0f-1a2b-4c3d-9e4f-5a6b7c8d9e0f";

        assertThat(hasher.hash(VISITOR_ID)).isNotEqualTo(hasher.hash(otherVisitorId));
    }

    @Test
    @DisplayName("비밀키가 다르면 같은 방문자 식별값도 다른 해시가 된다")
    void returnsDifferentHashForDifferentSecret() {
        VisitorKeyHasher otherHasher = hasher("b".repeat(32));

        assertThat(hasher.hash(VISITOR_ID)).isNotEqualTo(otherHasher.hash(VISITOR_ID));
    }

    @Test
    @DisplayName("해시에는 원래 방문자 식별값이 드러나지 않는다")
    void doesNotExposeVisitorId() {
        assertThat(hasher.hash(VISITOR_ID)).doesNotContain(VISITOR_ID);
    }

    @Test
    @DisplayName("방문자 식별값이 없으면 해시할 수 없다")
    void rejectsNullVisitorId() {
        assertThatThrownBy(() -> hasher.hash(null))
                .isInstanceOf(NullPointerException.class);
    }

    private static VisitorKeyHasher hasher(String secret) {
        return new VisitorKeyHasher(
                new VisitorProperties("VISITOR_ID", Duration.ofDays(365), true, "Lax", secret)
        );
    }
}

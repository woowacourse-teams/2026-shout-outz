package com.shoutoutz.api.auth.presentation.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class CsrfTokenManagerTest {

    private final CsrfTokenManager csrfTokenManager = new CsrfTokenManager();

    @Test
    @DisplayName("같은 세션에서는 동일한 CSRF 토큰을 반환한다")
    void reusesCsrfTokenInSameSession() {
        MockHttpSession session = new MockHttpSession();

        String first = csrfTokenManager.getOrCreate(session);
        String second = csrfTokenManager.getOrCreate(session);

        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("서로 다른 세션에는 다른 CSRF 토큰을 발급한다")
    void createsDifferentCsrfTokenForDifferentSessions() {
        String first = csrfTokenManager.getOrCreate(new MockHttpSession());
        String second = csrfTokenManager.getOrCreate(new MockHttpSession());

        assertThat(second).isNotEqualTo(first);
    }
}

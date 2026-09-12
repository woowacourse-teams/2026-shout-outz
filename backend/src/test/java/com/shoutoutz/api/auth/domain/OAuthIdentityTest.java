package com.shoutoutz.api.auth.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuthIdentityTest {

    @Test
    @DisplayName("OAuth 신원에는 Provider가 필요하다")
    void rejectsNullProvider() {
        assertThatThrownBy(() -> new OAuthIdentity(null, "12345678", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("OAuth 신원에는 Provider 계정 ID가 필요하다")
    void rejectsBlankProviderAccountId() {
        assertThatThrownBy(() -> new OAuthIdentity(OAuthProvider.GITHUB, " ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("OAuth 신원의 Provider 계정 ID는 255자를 초과할 수 없다")
    void rejectsProviderAccountIdOver255Characters() {
        assertThatThrownBy(() -> new OAuthIdentity(
                OAuthProvider.GITHUB,
                "a".repeat(256),
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }
}

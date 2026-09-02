package com.shoutoutz.api.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuthAccountTest {

    private static final Instant INITIAL_LOGIN_AT = Instant.parse("2026-09-02T00:00:00Z");

    @Test
    @DisplayName("OAuth 계정을 초기화하면 외부 계정 정보와 최초 로그인 시각을 가진다")
    void initializesOAuthAccount() {
        OAuthAccount account = OAuthAccount.initialize(
                1L,
                OAuthProvider.GITHUB,
                "12345678",
                "https://avatars.githubusercontent.com/u/12345678",
                INITIAL_LOGIN_AT
        );

        assertThat(account.getId()).isNull();
        assertThat(account.getUserId()).isEqualTo(1L);
        assertThat(account.getProvider()).isEqualTo(OAuthProvider.GITHUB);
        assertThat(account.getProviderAccountId()).isEqualTo("12345678");
        assertThat(account.getProviderAvatarUrl())
                .isEqualTo("https://avatars.githubusercontent.com/u/12345678");
        assertThat(account.getLastSyncedAt()).isEqualTo(INITIAL_LOGIN_AT);
        assertThat(account.getLastLoginAt()).isEqualTo(INITIAL_LOGIN_AT);
    }

    @Test
    @DisplayName("OAuth 계정에는 사용자 ID가 필요하다")
    void rejectsNullUserId() {
        assertThatThrownBy(() -> OAuthAccount.initialize(
                null,
                OAuthProvider.GITHUB,
                "12345678",
                null,
                INITIAL_LOGIN_AT
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("OAuth 계정에는 Provider가 필요하다")
    void rejectsNullProvider() {
        assertThatThrownBy(() -> OAuthAccount.initialize(
                1L,
                null,
                "12345678",
                null,
                INITIAL_LOGIN_AT
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Provider 계정 ID는 비어 있을 수 없다")
    void rejectsBlankProviderAccountId() {
        assertThatThrownBy(() -> OAuthAccount.initialize(
                1L,
                OAuthProvider.GITHUB,
                " ",
                null,
                INITIAL_LOGIN_AT
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Provider 계정 ID는 255자를 초과할 수 없다")
    void rejectsProviderAccountIdOver255Characters() {
        assertThatThrownBy(() -> OAuthAccount.initialize(
                1L,
                OAuthProvider.GITHUB,
                "a".repeat(256),
                null,
                INITIAL_LOGIN_AT
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("OAuth 계정 초기화에는 인증 시각이 필요하다")
    void rejectsNullAuthenticatedAtWhenInitializing() {
        assertThatThrownBy(() -> OAuthAccount.initialize(
                1L,
                OAuthProvider.GITHUB,
                "12345678",
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("OAuth 로그인을 기록하면 Provider 정보와 로그인 시각을 갱신한다")
    void recordsOAuthLogin() {
        OAuthAccount account = OAuthAccount.initialize(
                1L,
                OAuthProvider.GITHUB,
                "12345678",
                "https://avatars.githubusercontent.com/u/old",
                INITIAL_LOGIN_AT
        );
        Instant nextLoginAt = Instant.parse("2026-09-03T00:00:00Z");

        OAuthAccount loggedInAccount = account.recordLogin(
                "https://avatars.githubusercontent.com/u/new",
                nextLoginAt
        );

        assertThat(loggedInAccount.getProviderAvatarUrl())
                .isEqualTo("https://avatars.githubusercontent.com/u/new");
        assertThat(loggedInAccount.getLastSyncedAt()).isEqualTo(nextLoginAt);
        assertThat(loggedInAccount.getLastLoginAt()).isEqualTo(nextLoginAt);
        assertThat(account.getLastLoginAt()).isEqualTo(INITIAL_LOGIN_AT);
    }

    @Test
    @DisplayName("OAuth 로그인 기록에는 인증 시각이 필요하다")
    void rejectsNullAuthenticatedAtWhenRecordingLogin() {
        OAuthAccount account = OAuthAccount.initialize(
                1L,
                OAuthProvider.GITHUB,
                "12345678",
                null,
                INITIAL_LOGIN_AT
        );

        assertThatThrownBy(() -> account.recordLogin(null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

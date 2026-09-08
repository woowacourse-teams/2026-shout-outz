package com.shoutoutz.api.auth.presentation.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.auth.application.OAuthLoginAttempt;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.user.domain.UserRole;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class AuthSessionAccessorTest {

    private final AuthSessionAccessor authSessionAccessor = new AuthSessionAccessor();

    @Test
    @DisplayName("OAuth 로그인 시도는 세션에서 한 번만 가져올 수 있다")
    void consumesLoginAttemptOnce() {
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt attempt = new OAuthLoginAttempt(
                "state",
                "code-verifier",
                Instant.parse("2026-09-03T00:00:00Z")
        );
        authSessionAccessor.saveLoginAttempt(session, attempt);

        OAuthLoginAttempt consumed = authSessionAccessor.consumeLoginAttempt(session);

        assertThat(consumed).isEqualTo(attempt);
        assertThatThrownBy(() -> authSessionAccessor.consumeLoginAttempt(session))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("인증 정보를 저장하면 가입 대기 신원을 제거한다")
    void replacesPendingIdentityWithAuthentication() {
        MockHttpSession session = new MockHttpSession();
        authSessionAccessor.savePendingIdentity(session, githubIdentity());

        authSessionAccessor.saveAuthentication(session, 1L, UserRole.USER);

        assertThat(authSessionAccessor.findAuthentication(session)).isPresent();
        assertThat(authSessionAccessor.findPendingIdentity(session)).isEmpty();
    }

    @Test
    @DisplayName("가입 대기 신원을 저장하면 기존 인증 정보를 제거한다")
    void replacesAuthenticationWithPendingIdentity() {
        MockHttpSession session = new MockHttpSession();
        authSessionAccessor.saveAuthentication(session, 1L, UserRole.USER);

        authSessionAccessor.savePendingIdentity(session, githubIdentity());

        assertThat(authSessionAccessor.findAuthentication(session)).isEmpty();
        assertThat(authSessionAccessor.findPendingIdentity(session)).contains(githubIdentity());
    }

    @Test
    @DisplayName("인증 또는 가입 대기 정보가 있는 세션을 보호 대상으로 판단한다")
    void identifiesProtectedSession() {
        MockHttpSession unauthenticated = new MockHttpSession();
        MockHttpSession authenticated = new MockHttpSession();
        MockHttpSession signupPending = new MockHttpSession();
        authSessionAccessor.saveAuthentication(authenticated, 1L, UserRole.USER);
        authSessionAccessor.savePendingIdentity(signupPending, githubIdentity());

        assertThat(authSessionAccessor.requiresCsrfProtection(null)).isFalse();
        assertThat(authSessionAccessor.requiresCsrfProtection(unauthenticated)).isFalse();
        assertThat(authSessionAccessor.requiresCsrfProtection(authenticated)).isTrue();
        assertThat(authSessionAccessor.requiresCsrfProtection(signupPending)).isTrue();
    }

    private OAuthIdentity githubIdentity() {
        return new OAuthIdentity(OAuthProvider.GITHUB, "12345678", null);
    }
}

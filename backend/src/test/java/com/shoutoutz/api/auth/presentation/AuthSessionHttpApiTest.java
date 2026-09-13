package com.shoutoutz.api.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.auth.presentation.dto.response.AuthSessionResponse;
import com.shoutoutz.api.auth.presentation.security.CsrfTokenManager;
import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.auth.presentation.session.AuthSessionManager;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

class AuthSessionHttpApiTest {

    private final CsrfTokenManager csrfTokenManager = new CsrfTokenManager();
    private final AuthSessionAccessor authSessionAccessor = new AuthSessionAccessor();
    private final AuthSessionManager authSessionManager =
            new AuthSessionManager(authSessionAccessor);
    private final AuthSessionHttpApi authSessionHttpApi =
            new AuthSessionHttpApi(
                    csrfTokenManager,
                    authSessionAccessor,
                    authSessionManager
            );

    @Test
    @DisplayName("인증 정보가 없으면 비인증 상태와 CSRF 토큰을 반환한다")
    void returnsUnauthenticatedSession() {
        MockHttpSession session = new MockHttpSession();

        ResponseEntity<SuccessResponse<AuthSessionResponse>> response =
                authSessionHttpApi.getAuthSession(session);
        AuthSessionResponse data = response.getBody().data();

        assertThat(data.status()).isEqualTo(AuthSessionResponse.Status.UNAUTHENTICATED);
        assertThat(data.userId()).isNull();
        assertThat(data.role()).isNull();
        assertThat(data.csrfToken()).isNotBlank();
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    }

    @Test
    @DisplayName("OAuth 신원이 가입 대기 중이면 가입 필요 상태를 반환한다")
    void returnsSignupRequiredSession() {
        MockHttpSession session = new MockHttpSession();
        authSessionAccessor.savePendingIdentity(
                session,
                new OAuthIdentity(OAuthProvider.GITHUB, "12345678", null)
        );

        AuthSessionResponse response = authSessionHttpApi.getAuthSession(session).getBody().data();

        assertThat(response.status()).isEqualTo(AuthSessionResponse.Status.SIGNUP_REQUIRED);
        assertThat(response.userId()).isNull();
        assertThat(response.role()).isNull();
        assertThat(response.csrfToken()).isNotBlank();
    }

    @Test
    @DisplayName("인증 정보가 있으면 사용자 ID와 권한을 반환한다")
    void returnsAuthenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        authSessionAccessor.saveAuthentication(session, 1L, UserRole.USER);

        AuthSessionResponse response = authSessionHttpApi.getAuthSession(session).getBody().data();

        assertThat(response.status()).isEqualTo(AuthSessionResponse.Status.AUTHENTICATED);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.role()).isEqualTo(UserRole.USER);
        assertThat(response.csrfToken()).isNotBlank();
    }

    @Test
    @DisplayName("로그아웃하면 현재 인증 세션을 무효화하고 본문 없이 응답한다")
    void invalidatesAuthenticatedSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();
        authSessionAccessor.saveAuthentication(session, 1L, UserRole.USER);

        ResponseEntity<Void> response = authSessionHttpApi.logout(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(session.isInvalid()).isTrue();
    }
}

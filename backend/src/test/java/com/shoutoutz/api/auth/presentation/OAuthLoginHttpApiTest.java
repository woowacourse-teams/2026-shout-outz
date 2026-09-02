package com.shoutoutz.api.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.shoutoutz.api.auth.application.OAuthLoginAttempt;
import com.shoutoutz.api.auth.application.OAuthLoginCallbackResult;
import com.shoutoutz.api.auth.application.OAuthLoginService;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.user.domain.UserRole;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.ResponseEntity;

class OAuthLoginHttpApiTest {

    private final OAuthLoginService oauthLoginService = mock(OAuthLoginService.class);
    private final OAuthLoginHttpApi oauthLoginHttpApi = new OAuthLoginHttpApi(
            oauthLoginService,
            new OAuthLoginProperties(URI.create("http://localhost:3000/oauth/callback"))
    );

    @Test
    @DisplayName("기존 사용자 로그인에 성공하면 Session ID를 회전하고 인증 정보를 저장한다")
    void rotatesSessionIdAndStoresAuthentication() {
        MockHttpServletRequest request = callbackRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();
        String previousSessionId = session.getId();
        given(oauthLoginService.completeGitHubLogin(
                "authorization-code",
                "state",
                loginAttempt()
        )).willReturn(OAuthLoginCallbackResult.authenticated(1L, UserRole.USER));

        ResponseEntity<Void> response = oauthLoginHttpApi.callbackGitHub(
                "authorization-code",
                "state",
                request
        );

        AuthenticatedSession authenticatedSession =
                (AuthenticatedSession) session.getAttribute("authenticatedSession");
        assertThat(session.getId()).isNotEqualTo(previousSessionId);
        assertThat(authenticatedSession.userId()).isEqualTo(1L);
        assertThat(authenticatedSession.role()).isEqualTo(UserRole.USER);
        assertThat(session.getAttribute("oauthLoginAttempt")).isNull();
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("http://localhost:3000/oauth/callback"));
    }

    @Test
    @DisplayName("신규 OAuth 사용자면 검증된 신원을 가입 대기 세션에 저장한다")
    void storesPendingOAuthIdentityForNewUser() {
        MockHttpServletRequest request = callbackRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();
        OAuthIdentity identity = new OAuthIdentity(
                OAuthProvider.GITHUB,
                "12345678",
                "https://avatars.githubusercontent.com/u/12345678"
        );
        given(oauthLoginService.completeGitHubLogin(
                "authorization-code",
                "state",
                loginAttempt()
        )).willReturn(OAuthLoginCallbackResult.signupRequired(identity));

        oauthLoginHttpApi.callbackGitHub("authorization-code", "state", request);

        assertThat(session.getAttribute("pendingOAuthIdentity")).isEqualTo(identity);
        assertThat(session.getAttribute("authenticatedSession")).isNull();
        assertThat(session.getAttribute("oauthLoginAttempt")).isNull();
    }

    private MockHttpServletRequest callbackRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("oauthLoginAttempt", loginAttempt());
        return request;
    }

    private OAuthLoginAttempt loginAttempt() {
        return new OAuthLoginAttempt(
                "state",
                "code-verifier",
                Instant.parse("2026-09-03T00:00:00Z")
        );
    }
}

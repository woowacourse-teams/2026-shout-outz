package com.shoutoutz.api.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.shoutoutz.api.auth.application.OAuthLoginService;
import com.shoutoutz.api.auth.application.OAuthLoginAttempt;
import com.shoutoutz.api.auth.application.command.OAuthLoginCallbackResult;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.auth.presentation.session.AuthSessionManager;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.ResponseEntity;

class OAuthLoginHttpApiTest {

    private static final URI DEFAULT_COMPLETION_URI =
            URI.create("http://localhost:3000/oauth/callback");
    private static final URI VERCEL_COMPLETION_URI =
            URI.create("https://wooteco-career-qa.vercel.app");
    private OAuthLoginAttempt currentAttempt;
    private final OAuthLoginService oauthLoginService = mock(OAuthLoginService.class);
    private final AuthSessionAccessor authSessionAccessor = new AuthSessionAccessor();
    private final AuthSessionManager authSessionManager =
            new AuthSessionManager(authSessionAccessor);
    private final OAuthLoginHttpApi oauthLoginHttpApi = new OAuthLoginHttpApi(
            oauthLoginService,
            new OAuthLoginProperties(
                    DEFAULT_COMPLETION_URI,
                    List.of(DEFAULT_COMPLETION_URI, VERCEL_COMPLETION_URI)
            ),
            authSessionAccessor,
            authSessionManager
    );

    @Test
    @DisplayName("허용된 returnTo가 있으면 해당 프론트엔드로 이동할 OAuth 시도를 저장한다")
    void startsLoginWithAllowedCompletionUri() {
        MockHttpSession session = new MockHttpSession();
        given(oauthLoginService.generateGitHubAuthorizationUri(any()))
                .willReturn(URI.create("https://github.com/login/oauth/authorize?client_id=client-id"));

        ResponseEntity<Void> response =
                oauthLoginHttpApi.authorizeGitHub(VERCEL_COMPLETION_URI, session);

        ArgumentCaptor<OAuthLoginAttempt> attempt = ArgumentCaptor.forClass(OAuthLoginAttempt.class);
        verify(oauthLoginService).generateGitHubAuthorizationUri(attempt.capture());
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("https://github.com/login/oauth/authorize?client_id=client-id"));
        assertThat(attempt.getValue().completionUri()).isEqualTo(VERCEL_COMPLETION_URI);
        assertThat(authSessionAccessor.beginCallbackProcessing(session, attempt.getValue().state()))
                .isSameAs(attempt.getValue());
    }

    @Test
    @DisplayName("WAITING 재시작은 새 returnTo 대신 최초 시도로 인가 URI를 생성한다")
    void resumesAttemptWithOriginalCompletionUri() {
        MockHttpSession session = new MockHttpSession();
        given(oauthLoginService.generateGitHubAuthorizationUri(any()))
                .willReturn(URI.create("https://github.com/login/oauth/authorize?client_id=client-id"));

        oauthLoginHttpApi.authorizeGitHub(VERCEL_COMPLETION_URI, session);
        oauthLoginHttpApi.authorizeGitHub(DEFAULT_COMPLETION_URI, session);

        ArgumentCaptor<OAuthLoginAttempt> attempt = ArgumentCaptor.forClass(OAuthLoginAttempt.class);
        verify(oauthLoginService, times(2)).generateGitHubAuthorizationUri(attempt.capture());
        assertThat(attempt.getAllValues().get(1)).isSameAs(attempt.getAllValues().getFirst());
        assertThat(attempt.getValue().completionUri()).isEqualTo(VERCEL_COMPLETION_URI);
    }

    @Test
    @DisplayName("허용되지 않은 returnTo는 OAuth 로그인을 시작하지 않는다")
    void rejectsUnallowedCompletionUri() {
        URI maliciousCompletionUri = URI.create("https://evil.example.com");

        assertThatThrownBy(() -> oauthLoginHttpApi.authorizeGitHub(
                maliciousCompletionUri,
                new MockHttpSession()
        ))
                .isInstanceOf(BadRequestException.class)
                .satisfies(exception -> assertThat(
                        ((BadRequestException) exception).getErrorCode().name()
                ).isEqualTo("OAUTH_COMPLETION_URI_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("기존 사용자 로그인에 성공하면 Session ID를 회전하고 인증 정보를 저장한다")
    void rotatesSessionIdAndStoresAuthentication() {
        MockHttpServletRequest request = callbackRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();
        String previousSessionId = session.getId();
        given(oauthLoginService.completeGitHubLogin(
                "authorization-code",
                loginAttempt().state(),
                loginAttempt()
        )).willReturn(OAuthLoginCallbackResult.authenticated(1L, UserRole.USER));

        ResponseEntity<Void> response = oauthLoginHttpApi.callbackGitHub(
                "authorization-code",
                loginAttempt().state(),
                request
        );

        AuthenticatedSession authenticatedSession = authSessionAccessor.findAuthentication(session)
                .orElseThrow();
        assertThat(session.getId()).isNotEqualTo(previousSessionId);
        assertThat(authenticatedSession.userId()).isEqualTo(1L);
        assertThat(authenticatedSession.role()).isEqualTo(UserRole.USER);
        assertThatThrownBy(() -> authSessionAccessor.beginCallbackProcessing(session, loginAttempt().state()))
                .isInstanceOf(BadRequestException.class);
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("http://localhost:3000/oauth/callback"));
    }

    @Test
    @DisplayName("신규 OAuth 사용자면 검증된 신원을 가입 대기 세션에 저장한다")
    void storesPendingOAuthIdentityForNewUser() {
        MockHttpServletRequest request = callbackRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();
        String previousSessionId = session.getId();
        OAuthIdentity identity = new OAuthIdentity(
                OAuthProvider.GITHUB,
                "12345678",
                "https://avatars.githubusercontent.com/u/12345678"
        );
        given(oauthLoginService.completeGitHubLogin(
                "authorization-code",
                loginAttempt().state(),
                loginAttempt()
        )).willReturn(OAuthLoginCallbackResult.signupRequired(identity));

        oauthLoginHttpApi.callbackGitHub("authorization-code", loginAttempt().state(), request);

        assertThat(session.getId()).isNotEqualTo(previousSessionId);
        assertThat(authSessionAccessor.findPendingIdentity(session)).contains(identity);
        assertThat(authSessionAccessor.findAuthentication(session)).isEmpty();
        assertThatThrownBy(() -> authSessionAccessor.beginCallbackProcessing(session, loginAttempt().state()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("로그인 처리에 실패하면 세션을 회전하지 않고 시도를 정리해 재시작을 허용한다")
    void cleansAttemptAfterLoginFailure() {
        MockHttpServletRequest request = callbackRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();
        String previousSessionId = session.getId();
        RuntimeException failure = new IllegalStateException("GitHub 응답 실패");
        given(oauthLoginService.completeGitHubLogin(
                "authorization-code",
                loginAttempt().state(),
                loginAttempt()
        )).willThrow(failure);

        assertThatThrownBy(() -> oauthLoginHttpApi.callbackGitHub(
                "authorization-code",
                loginAttempt().state(),
                request
        )).isSameAs(failure);

        assertThat(session.getId()).isEqualTo(previousSessionId);
        assertThat(authSessionAccessor.findAuthentication(session)).isEmpty();
        assertThat(authSessionAccessor.findPendingIdentity(session)).isEmpty();
        OAuthLoginAttempt replacement = authSessionAccessor.getOrCreateLoginAttempt(
                session,
                DEFAULT_COMPLETION_URI
        );
        assertThat(authSessionAccessor.beginCallbackProcessing(session, replacement.state()))
                .isEqualTo(replacement);
    }

    @Test
    @DisplayName("GitHub OAuth 동의 거절 시 state를 검증하고 로그인 시도를 소비한다")
    void validatesStateAndConsumesAttemptWhenGitHubAuthorizationIsDenied() {
        MockHttpServletRequest request = callbackRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();

        ResponseEntity<Void> response = oauthLoginHttpApi.handleDeniedGitHubAuthorization(
                loginAttempt().state(),
                request
        );

        verify(oauthLoginService).validateGitHubCallback(loginAttempt().state(), loginAttempt());
        assertThat(authSessionAccessor.findAuthentication(session)).isEmpty();
        assertThat(authSessionAccessor.findPendingIdentity(session)).isEmpty();
        assertThatThrownBy(() -> authSessionAccessor.beginCallbackProcessing(session, loginAttempt().state()))
                .isInstanceOf(BadRequestException.class);
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("http://localhost:3000/oauth/callback"));
    }

    @Test
    @DisplayName("로그인 성공 Callback의 state가 다르면 로그인 시도를 유지한다")
    void preservesAttemptWhenSuccessfulCallbackStateDoesNotMatch() {
        MockHttpServletRequest request = callbackRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();

        assertThatThrownBy(() -> oauthLoginHttpApi.callbackGitHub(
                "authorization-code",
                "other-state",
                request
        )).isInstanceOf(BadRequestException.class);

        assertThat(authSessionAccessor.beginCallbackProcessing(session, loginAttempt().state()))
                .isEqualTo(loginAttempt());
    }

    @Test
    @DisplayName("로그인 거절 Callback의 state가 다르면 로그인 시도를 유지한다")
    void preservesAttemptWhenDeniedCallbackStateDoesNotMatch() {
        MockHttpServletRequest request = callbackRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();

        assertThatThrownBy(() -> oauthLoginHttpApi.handleDeniedGitHubAuthorization(
                "other-state",
                request
        )).isInstanceOf(BadRequestException.class);

        assertThat(authSessionAccessor.beginCallbackProcessing(session, loginAttempt().state()))
                .isEqualTo(loginAttempt());
    }

    private MockHttpServletRequest callbackRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        currentAttempt = authSessionAccessor.getOrCreateLoginAttempt(request.getSession(), DEFAULT_COMPLETION_URI);
        return request;
    }

    private OAuthLoginAttempt loginAttempt() {
        return currentAttempt;
    }
}

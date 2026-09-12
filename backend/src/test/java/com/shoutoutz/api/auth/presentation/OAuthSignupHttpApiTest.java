package com.shoutoutz.api.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.shoutoutz.api.auth.application.OAuthSignupService;
import com.shoutoutz.api.auth.application.dto.command.OAuthSignupCommand;
import com.shoutoutz.api.auth.application.dto.result.OAuthSignupResult;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.auth.presentation.dto.request.OAuthSignupRequest;
import com.shoutoutz.api.auth.presentation.dto.response.OAuthSignupResponse;
import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.auth.presentation.session.AuthSessionManager;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.user.domain.UserRole;
import com.shoutoutz.api.user.domain.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class OAuthSignupHttpApiTest {

    private final OAuthSignupService oauthSignupService = mock(OAuthSignupService.class);
    private final AuthSessionAccessor authSessionAccessor = new AuthSessionAccessor();
    private final AuthSessionManager authSessionManager =
            new AuthSessionManager(authSessionAccessor);
    private final OAuthSignupHttpApi oauthSignupHttpApi = new OAuthSignupHttpApi(
            oauthSignupService,
            authSessionAccessor,
            authSessionManager
    );

    @Test
    @DisplayName("OAuth 가입을 완료하면 Session ID를 회전하고 인증 정보를 저장한다")
    void rotatesSessionIdAndStoresAuthenticationAfterSignup() {
        OAuthIdentity identity = githubIdentity();
        OAuthSignupRequest signupRequest = signupRequest();
        OAuthSignupCommand command = signupRequest.toCommand(identity);
        MockHttpServletRequest request = signupHttpRequest(identity);
        MockHttpSession session = (MockHttpSession) request.getSession();
        String previousSessionId = session.getId();
        given(oauthSignupService.signup(command))
                .willReturn(new OAuthSignupResult(1L, UserRole.USER));

        ResponseEntity<SuccessResponse<OAuthSignupResponse>> response = oauthSignupHttpApi.signup(
                signupRequest,
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data().userId()).isEqualTo(1L);
        assertThat(session.getId()).isNotEqualTo(previousSessionId);
        assertThat(authSessionAccessor.findAuthentication(session)).isPresent();
        assertThat(authSessionAccessor.findPendingIdentity(session)).isEmpty();
    }

    @Test
    @DisplayName("가입 대기 OAuth 신원이 없으면 가입 요청을 거부한다")
    void rejectsSignupWithoutPendingOAuthIdentity() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> oauthSignupHttpApi.signup(signupRequest(), request))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(oauthSignupService);
    }

    @Test
    @DisplayName("가입 처리에 실패하면 OAuth 가입 대기 상태를 유지한다")
    void preservesPendingIdentityWhenSignupFails() {
        OAuthIdentity identity = githubIdentity();
        OAuthSignupRequest signupRequest = signupRequest();
        MockHttpServletRequest request = signupHttpRequest(identity);
        MockHttpSession session = (MockHttpSession) request.getSession();
        String previousSessionId = session.getId();
        given(oauthSignupService.signup(signupRequest.toCommand(identity)))
                .willThrow(new IllegalStateException("가입 처리 실패"));

        assertThatThrownBy(() -> oauthSignupHttpApi.signup(signupRequest, request))
                .isInstanceOf(IllegalStateException.class);

        assertThat(session.getId()).isEqualTo(previousSessionId);
        assertThat(authSessionAccessor.findAuthentication(session)).isEmpty();
        assertThat(authSessionAccessor.findPendingIdentity(session)).contains(identity);
    }

    private MockHttpServletRequest signupHttpRequest(OAuthIdentity identity) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        authSessionAccessor.savePendingIdentity(request.getSession(), identity);
        return request;
    }

    private OAuthSignupRequest signupRequest() {
        return new OAuthSignupRequest(
                "sangjun",
                "상준",
                UserType.GENERAL,
                null,
                null
        );
    }

    private OAuthIdentity githubIdentity() {
        return new OAuthIdentity(
                OAuthProvider.GITHUB,
                "12345678",
                "https://avatars.githubusercontent.com/u/12345678"
        );
    }
}

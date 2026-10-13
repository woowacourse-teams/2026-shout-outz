package com.shoutoutz.api.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.shoutoutz.api.auth.application.LoadTestLoginService;
import com.shoutoutz.api.auth.presentation.dto.request.LoadTestLoginRequest;
import com.shoutoutz.api.auth.presentation.dto.response.AuthSessionResponse;
import com.shoutoutz.api.auth.presentation.security.CsrfTokenManager;
import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.auth.presentation.session.AuthSessionManager;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.account.UserStatus;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class LoadTestLoginHttpApiTest {

    private final LoadTestLoginService loadTestLoginService = mock(LoadTestLoginService.class);
    private final CsrfTokenManager csrfTokenManager = new CsrfTokenManager();
    private final AuthSessionAccessor authSessionAccessor = new AuthSessionAccessor();
    private final AuthSessionManager authSessionManager =
            new AuthSessionManager(authSessionAccessor);
    private final LoadTestLoginHttpApi loadTestLoginHttpApi = new LoadTestLoginHttpApi(
            loadTestLoginService,
            authSessionManager,
            csrfTokenManager
    );

    @BeforeEach
    void setUp() {
        given(loadTestLoginService.findLoginUser("lt-00001")).willReturn(User.builder()
                .id(1L)
                .handle("@lt-00001")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build());
    }

    @Test
    @DisplayName("세션이 없으면 새로 만들어 로그인 정보를 저장한다.")
    void createsAuthenticatedSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        loadTestLoginHttpApi.login(new LoadTestLoginRequest("lt-00001"), request);

        HttpSession session = request.getSession(false);
        assertThat(session).isNotNull();
        assertThat(authSessionAccessor.findAuthentication(session))
                .contains(new AuthenticatedSession(1L, UserRole.USER));
    }

    @Test
    @DisplayName("기존 세션이 있으면 세션 id를 바꿔 로그인한다.")
    void rotatesExistingSessionId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String previousSessionId = request.getSession().getId();

        loadTestLoginHttpApi.login(new LoadTestLoginRequest("lt-00001"), request);

        assertThat(request.getSession(false).getId()).isNotEqualTo(previousSessionId);
    }

    @Test
    @DisplayName("로그인 상태와 사용자 정보, 세션의 CSRF 토큰을 반환한다.")
    void returnsAuthenticatedSessionResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<SuccessResponse<AuthSessionResponse>> response =
                loadTestLoginHttpApi.login(new LoadTestLoginRequest("lt-00001"), request);
        AuthSessionResponse data = response.getBody().data();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(data.status()).isEqualTo(AuthSessionResponse.Status.AUTHENTICATED);
        assertThat(data.userId()).isEqualTo(1L);
        assertThat(data.role()).isEqualTo(UserRole.USER);
        assertThat(data.csrfToken())
                .isEqualTo(csrfTokenManager.getOrCreate(request.getSession(false)));
    }
}

package com.shoutoutz.api.auth.presentation.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.user.domain.account.UserRole;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import tools.jackson.databind.json.JsonMapper;

class CsrfProtectionFilterTest {

    private final CsrfTokenManager csrfTokenManager = new CsrfTokenManager();
    private final AuthSessionAccessor authSessionAccessor = new AuthSessionAccessor();
    private final CsrfProtectionFilter csrfProtectionFilter =
            new CsrfProtectionFilter(
                    csrfTokenManager,
                    authSessionAccessor,
                    JsonMapper.builder().build()
            );

    @Test
    @DisplayName("로그인 사용자의 상태 변경 요청에 CSRF 토큰이 없으면 거부한다")
    void rejectsStateChangingRequestWithoutCsrfToken() throws ServletException, IOException {
        MockHttpServletRequest request = authenticatedPostRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        csrfProtectionFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains(
                "\"status\":\"error\"",
                "\"code\":\"CSRF_TOKEN_INVALID\""
        );
        assertThat(filterChain.getRequest()).isNull();
    }

    @Test
    @DisplayName("가입 대기 사용자의 상태 변경 요청에 CSRF 토큰이 없으면 거부한다")
    void rejectsSignupRequestWithoutCsrfToken() throws ServletException, IOException {
        MockHttpServletRequest request = signupPendingPostRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        csrfProtectionFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(filterChain.getRequest()).isNull();
    }

    @Test
    @DisplayName("로그인 사용자의 상태 변경 요청에 잘못된 CSRF 토큰이 있으면 거부한다")
    void rejectsStateChangingRequestWithInvalidCsrfToken() throws ServletException, IOException {
        MockHttpServletRequest request = authenticatedPostRequest();
        csrfTokenManager.getOrCreate(request.getSession());
        request.addHeader(CsrfProtectionFilter.CSRF_HEADER, "invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        csrfProtectionFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(filterChain.getRequest()).isNull();
    }

    @Test
    @DisplayName("로그인 사용자의 상태 변경 요청에 올바른 CSRF 토큰이 있으면 통과시킨다")
    void allowsStateChangingRequestWithValidCsrfToken() throws ServletException, IOException {
        MockHttpServletRequest request = authenticatedPostRequest();
        String csrfToken = csrfTokenManager.getOrCreate(request.getSession());
        request.addHeader(CsrfProtectionFilter.CSRF_HEADER, csrfToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        csrfProtectionFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("인증 정보가 없는 사용자의 공개 상태 변경 요청은 통과시킨다")
    void allowsUnauthenticatedStateChangingRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/v1/visitors"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        csrfProtectionFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("조회 요청에는 CSRF 토큰을 요구하지 않는다")
    void allowsSafeRequestWithoutCsrfToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/auth/session"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        csrfProtectionFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    private MockHttpServletRequest authenticatedPostRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/v1/projects"
        );
        MockHttpSession session = new MockHttpSession();
        authSessionAccessor.saveAuthentication(session, 1L, UserRole.USER);
        request.setSession(session);
        return request;
    }

    private MockHttpServletRequest signupPendingPostRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/v1/auth/signup"
        );
        MockHttpSession session = new MockHttpSession();
        authSessionAccessor.savePendingIdentity(
                session,
                new OAuthIdentity(OAuthProvider.GITHUB, "12345678", null)
        );
        request.setSession(session);
        return request;
    }
}

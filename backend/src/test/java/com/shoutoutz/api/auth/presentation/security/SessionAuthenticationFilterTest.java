package com.shoutoutz.api.auth.presentation.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.user.domain.account.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SessionAuthenticationFilterTest {

    private final AuthSessionAccessor authSessionAccessor = new AuthSessionAccessor();
    private final SessionAuthenticationFilter filter =
            new SessionAuthenticationFilter(authSessionAccessor);

    @Test
    @DisplayName("인증 세션이 있으면 로그인 사용자 정보를 요청에 저장한다")
    void storesAuthenticationInRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects");
        authSessionAccessor.saveAuthentication(request.getSession(), 1L, UserRole.USER);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getAttribute(AuthenticatedSession.class.getName()))
                .isEqualTo(new AuthenticatedSession(1L, UserRole.USER));
    }

    @Test
    @DisplayName("인증 세션이 없어도 공개 API 요청은 계속 진행한다")
    void continuesPublicRequestWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects");
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(request.getAttribute(AuthenticatedSession.class.getName())).isNull();
        assertThat(filterChain.getRequest()).isSameAs(request);
    }
}

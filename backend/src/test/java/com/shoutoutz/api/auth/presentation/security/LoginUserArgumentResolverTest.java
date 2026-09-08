package com.shoutoutz.api.auth.presentation.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.exception.custom.UnauthorizedException;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class LoginUserArgumentResolverTest {

    private final LoginUserArgumentResolver resolver = new LoginUserArgumentResolver();

    @Test
    @DisplayName("로그인 사용자 어노테이션이 붙은 인증 사용자 타입을 지원한다")
    void supportsLoginUser() throws NoSuchMethodException {
        MethodParameter parameter = loginUserParameter();

        assertThat(resolver.supportsParameter(parameter)).isTrue();
    }

    @Test
    @DisplayName("요청의 인증 세션에서 사용자 ID와 권한을 반환한다")
    void resolvesAuthenticatedUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(
                SessionAuthenticationFilter.AUTHENTICATED_SESSION_ATTRIBUTE,
                new AuthenticatedSession(1L, UserRole.ADMIN)
        );

        Object resolved = resolver.resolveArgument(
                loginUserParameter(),
                null,
                new ServletWebRequest(request),
                null
        );

        assertThat(resolved).isEqualTo(new AuthenticatedUser(1L, UserRole.ADMIN));
    }

    @Test
    @DisplayName("요청에 인증 정보가 없으면 로그인이 필요하다고 응답한다")
    void rejectsUnauthenticatedRequest() throws NoSuchMethodException {
        ServletWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());

        assertThatThrownBy(() -> resolver.resolveArgument(
                loginUserParameter(),
                null,
                webRequest,
                null
        ))
                .isInstanceOf(UnauthorizedException.class);
    }

    private MethodParameter loginUserParameter() throws NoSuchMethodException {
        Method method = TestHttpApi.class.getDeclaredMethod(
                "getAuthenticatedUser",
                AuthenticatedUser.class
        );
        return new MethodParameter(method, 0);
    }

    private static class TestHttpApi {

        @SuppressWarnings("unused")
        void getAuthenticatedUser(@LoginUser AuthenticatedUser authenticatedUser) {
        }
    }
}

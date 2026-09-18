package com.shoutoutz.api.visitor.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class VisitorKeyArgumentResolverTest {

    private final VisitorKeyArgumentResolver resolver = new VisitorKeyArgumentResolver();

    @Test
    @DisplayName("방문자 어노테이션이 붙은 방문자 키 타입을 지원한다")
    void supportsVisitorKey() throws NoSuchMethodException {
        assertThat(resolver.supportsParameter(parameter("getVisitorKey", VisitorKey.class))).isTrue();
    }

    @Test
    @DisplayName("방문자 어노테이션이 없거나 타입이 다르면 지원하지 않는다")
    void doesNotSupportOtherParameters() throws NoSuchMethodException {
        assertThat(resolver.supportsParameter(parameter("getWithoutAnnotation", VisitorKey.class))).isFalse();
        assertThat(resolver.supportsParameter(parameter("getString", String.class))).isFalse();
    }

    @Test
    @DisplayName("필터가 요청 속성에 담은 방문자 키를 반환한다")
    void resolvesVisitorKey() throws Exception {
        VisitorKey visitorKey = new VisitorKey("hashed-visitor-key");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(VisitorCookieFilter.VISITOR_KEY_ATTRIBUTE, visitorKey);

        Object resolved = resolver.resolveArgument(
                parameter("getVisitorKey", VisitorKey.class),
                null,
                new ServletWebRequest(request),
                null
        );

        assertThat(resolved).isEqualTo(visitorKey);
    }

    @Test
    @DisplayName("요청 속성에 방문자 키가 없으면 필터 설정 오류로 본다")
    void rejectsRequestWithoutVisitorKey() throws NoSuchMethodException {
        MethodParameter parameter = parameter("getVisitorKey", VisitorKey.class);
        ServletWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());

        assertThatThrownBy(() -> resolver.resolveArgument(parameter, null, webRequest, null))
                .isInstanceOf(IllegalStateException.class);
    }

    private MethodParameter parameter(String methodName, Class<?> type) throws NoSuchMethodException {
        Method method = TestHttpApi.class.getDeclaredMethod(methodName, type);
        return new MethodParameter(method, 0);
    }

    private static class TestHttpApi {

        @SuppressWarnings("unused")
        void getVisitorKey(@Visitor VisitorKey visitorKey) {
        }

        @SuppressWarnings("unused")
        void getWithoutAnnotation(VisitorKey visitorKey) {
        }

        @SuppressWarnings("unused")
        void getString(@Visitor String visitorKey) {
        }
    }
}

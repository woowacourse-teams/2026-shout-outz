package com.shoutoutz.api.visitor.presentation;

import com.shoutoutz.api.visitor.domain.VisitorKey;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * VisitorCookieFilter 가 요청 속성에 담아 둔 방문자 키를 꺼내 준다.
 * 해시는 필터에서 끝나므로 의존성 없이 읽기만 한다.
 */
@Component
class VisitorKeyArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Visitor.class)
                && parameter.getParameterType().equals(VisitorKey.class);
    }

    /**
     * 필터는 모든 API 요청에 방문자 키를 담으므로, 없으면 필터 설정이 잘못된 것이다.
     */
    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new IllegalStateException("HTTP 요청을 찾을 수 없습니다.");
        }

        Object value = request.getAttribute(VisitorCookieFilter.VISITOR_KEY_ATTRIBUTE);
        if (value instanceof VisitorKey visitorKey) {
            return visitorKey;
        }
        throw new IllegalStateException("방문자 키를 찾을 수 없습니다.");
    }
}

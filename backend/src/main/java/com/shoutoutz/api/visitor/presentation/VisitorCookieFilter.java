package com.shoutoutz.api.visitor.presentation;

import com.shoutoutz.api.visitor.VisitorProperties;
import com.shoutoutz.api.visitor.application.VisitorKeyHasher;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseCookie;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * API 요청마다 방문자 식별 쿠키를 확인하고, 없거나 형식이 잘못됐으면 새로 발급한다.
 * 새로 발급한 요청에서도 방문자를 식별할 수 있도록, 최종 식별값을 해시한 방문자 키를 요청 속성에 담는다.
 * 쿠키 원래 값은 요청 속성에 남기지 않는다.
 */
@RequiredArgsConstructor
class VisitorCookieFilter extends OncePerRequestFilter {

    static final String VISITOR_KEY_ATTRIBUTE = VisitorKey.class.getName();

    private static final String API_PATH = "/api/";
    private static final String COOKIE_PATH = "/";

    private final VisitorProperties properties;
    private final VisitorKeyHasher visitorKeyHasher;

    /**
     * CORS 사전 요청(OPTIONS)은 브라우저가 쿠키를 싣지 않으므로 발급하지 않는다.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(API_PATH)
                || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String visitorId = findVisitorId(request)
                .orElseGet(() -> issueVisitorId(response));
        request.setAttribute(VISITOR_KEY_ATTRIBUTE, new VisitorKey(visitorKeyHasher.hash(visitorId)));

        filterChain.doFilter(request, response);
    }

    private Optional<String> findVisitorId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> properties.cookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(VisitorCookieFilter::isValidVisitorId)
                .findFirst();
    }

    /**
     * 서버가 발급한 형식(소문자 UUID)과 정확히 같을 때만 인정한다.
     * UUID.fromString 은 "1-1-1-1-1" 같은 값도 받아들이므로, 다시 문자열로 바꿔 비교한다.
     */
    private static boolean isValidVisitorId(String value) {
        try {
            return UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String issueVisitorId(HttpServletResponse response) {
        String visitorId = UUID.randomUUID().toString();
        ResponseCookie cookie = ResponseCookie.from(properties.cookieName(), visitorId)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(properties.cookieSameSite())
                .path(COOKIE_PATH)
                .maxAge(properties.cookieMaxAge())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return visitorId;
    }
}

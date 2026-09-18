package com.shoutoutz.api.visitor.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.visitor.VisitorProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class VisitorCookieFilterTest {

    private static final String COOKIE_NAME = "VISITOR_ID";
    private static final String VISITOR_ID = "3f2a1b4c-5d6e-4f70-8a9b-0c1d2e3f4a5b";

    private final VisitorCookieFilter filter = new VisitorCookieFilter(
            new VisitorProperties(COOKIE_NAME, Duration.ofDays(365), true, "Lax", "a".repeat(32))
    );

    @Test
    @DisplayName("방문자 쿠키가 없으면 새 식별값을 발급하고 요청 속성에 담는다")
    void issuesVisitorIdWhenCookieIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = apiRequest("POST");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        String issuedVisitorId = response.getCookie(COOKIE_NAME).getValue();
        assertThat(UUID.fromString(issuedVisitorId).toString()).isEqualTo(issuedVisitorId);
        assertThat(request.getAttribute(VisitorCookieFilter.VISITOR_ID_ATTRIBUTE)).isEqualTo(issuedVisitorId);
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("발급한 쿠키는 HttpOnly, Secure, SameSite, 경로, 만료 기간을 설정값대로 가진다")
    void issuesCookieWithConfiguredAttributes() throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(apiRequest("GET"), response, new MockFilterChain());

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                .startsWith(COOKIE_NAME + "=")
                .contains("Path=/", "Max-Age=31536000", "Secure", "HttpOnly", "SameSite=Lax");
    }

    @Test
    @DisplayName("올바른 방문자 쿠키가 있으면 새로 발급하지 않고 그 값을 요청 속성에 담는다")
    void keepsValidVisitorId() throws ServletException, IOException {
        MockHttpServletRequest request = apiRequest("POST");
        request.setCookies(new Cookie(COOKIE_NAME, VISITOR_ID));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(request.getAttribute(VisitorCookieFilter.VISITOR_ID_ATTRIBUTE)).isEqualTo(VISITOR_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-a-uuid", "1-1-1-1-1", "3F2A1B4C-5D6E-4F70-8A9B-0C1D2E3F4A5B"})
    @DisplayName("방문자 쿠키가 서버가 발급한 형식이 아니면 새로 발급한다")
    void reissuesInvalidVisitorId(String invalidVisitorId) throws ServletException, IOException {
        MockHttpServletRequest request = apiRequest("POST");
        request.setCookies(new Cookie(COOKIE_NAME, invalidVisitorId));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String issuedVisitorId = response.getCookie(COOKIE_NAME).getValue();
        assertThat(issuedVisitorId).isNotEqualTo(invalidVisitorId);
        assertThat(request.getAttribute(VisitorCookieFilter.VISITOR_ID_ATTRIBUTE)).isEqualTo(issuedVisitorId);
    }

    @Test
    @DisplayName("같은 이름의 쿠키가 여러 개면 올바른 첫 번째 값을 쓴다")
    void usesFirstValidVisitorId() throws ServletException, IOException {
        MockHttpServletRequest request = apiRequest("POST");
        request.setCookies(new Cookie(COOKIE_NAME, "not-a-uuid"), new Cookie(COOKIE_NAME, VISITOR_ID));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(request.getAttribute(VisitorCookieFilter.VISITOR_ID_ATTRIBUTE)).isEqualTo(VISITOR_ID);
    }

    @Test
    @DisplayName("API 요청이 아니면 쿠키를 발급하지 않는다")
    void ignoresNonApiRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/docs/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(request.getAttribute(VisitorCookieFilter.VISITOR_ID_ATTRIBUTE)).isNull();
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("CORS 사전 요청에는 쿠키를 발급하지 않는다")
    void ignoresPreflightRequest() throws ServletException, IOException {
        MockHttpServletRequest request = apiRequest("OPTIONS");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(request.getAttribute(VisitorCookieFilter.VISITOR_ID_ATTRIBUTE)).isNull();
    }

    private MockHttpServletRequest apiRequest(String method) {
        return new MockHttpServletRequest(method, "/api/v1/projects/1/views");
    }
}

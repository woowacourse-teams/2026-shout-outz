package com.shoutoutz.api.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 요청당 한 번만 요청 추적 정보와 처리 결과를 기록하는 필터
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    static final String TRACE_ID_HEADER = "X-Trace-Id";
    static final String TRACE_ID_MDC_KEY = "traceId";
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final String NO_ROUTE = "-";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request.getHeader(TRACE_ID_HEADER));
        String previousTraceId = MDC.get(TRACE_ID_MDC_KEY);
        long startTime = System.nanoTime();

        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMillis = (System.nanoTime() - startTime) / 1_000_000;
            logRequest(request, response, durationMillis, traceId);
            restoreTraceId(previousTraceId);
        }
    }

    private String resolveTraceId(String requestedTraceId) {
        if (requestedTraceId != null && SAFE_TRACE_ID.matcher(requestedTraceId).matches()) {
            return requestedTraceId;
        }
        return UUID.randomUUID().toString();
    }

    private void logRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            long durationMillis,
            String traceId) {
        // 로그 기반 지표 필터가 기존 필드 순서에 의존할 수 있으므로 새 필드는 끝에 덧붙인다.
        String message = "http_request method={} path={} status={} duration_ms={} trace_id={} route={}";
        Object[] arguments = {
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                durationMillis,
                traceId,
                resolveRoute(request)
        };

        if (response.getStatus() >= 500) {
            log.error(message, arguments);
            return;
        }
        if (response.getStatus() >= 400) {
            log.warn(message, arguments);
            return;
        }
        log.info(message, arguments);
    }

    /**
     * 요청을 처리한 핸들러의 URL 템플릿을 반환한다.
     * 없는 주소는 정적 리소스 핸들러가 받으므로 {@code /**}로 기록된다.
     * 핸들러에 닿기 전에 끝난 요청(보안 필터 차단 등)은 템플릿이 없으므로 {@code -}를 반환한다.
     */
    private String resolveRoute(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern == null) {
            return NO_ROUTE;
        }
        return pattern.toString();
    }

    private void restoreTraceId(String previousTraceId) {
        if (previousTraceId == null) {
            MDC.remove(TRACE_ID_MDC_KEY);
            return;
        }
        MDC.put(TRACE_ID_MDC_KEY, previousTraceId);
    }
}

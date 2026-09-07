package com.shoutoutz.api.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HttpRequestLoggingFilterTest {

    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final HttpRequestLoggingFilter filter = new HttpRequestLoggingFilter();
    private Logger logger;
    private ListAppender<ILoggingEvent> logAppender;
    private Level previousLevel;

    @BeforeEach
    void setUp() {
        MDC.clear();
        logger = (Logger) LoggerFactory.getLogger(HttpRequestLoggingFilter.class);
        previousLevel = logger.getLevel();
        logger.setLevel(Level.ALL);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logger.setLevel(previousLevel);
        MDC.clear();
    }

    @Test
    @DisplayName("안전한 traceId를 응답 헤더와 MDC에 전달한다")
    void preservesSafeIncomingTraceIdInResponseAndMdcDuringRequest() throws Exception {
        String traceId = "trace-123_abc";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        request.addHeader(HttpRequestLoggingFilter.TRACE_ID_HEADER, traceId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] traceIdSeenInFilterChain = new String[1];
        FilterChain filterChain = (servletRequest, servletResponse) -> {
            traceIdSeenInFilterChain[0] = MDC.get(HttpRequestLoggingFilter.TRACE_ID_MDC_KEY);
            ((MockHttpServletResponse) servletResponse).setStatus(200);
        };

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader(HttpRequestLoggingFilter.TRACE_ID_HEADER)).isEqualTo(traceId);
        assertThat(traceIdSeenInFilterChain[0]).isEqualTo(traceId);
        assertThat(MDC.get(HttpRequestLoggingFilter.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("유효하지 않은 traceId가 전달되면 안전한 traceId를 새로 생성한다")
    void generatesSafeTraceIdWhenIncomingTraceIdIsInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        request.addHeader(HttpRequestLoggingFilter.TRACE_ID_HEADER, "trace\nwith-invalid-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        String traceId = response.getHeader(HttpRequestLoggingFilter.TRACE_ID_HEADER);
        assertThat(traceId).matches(SAFE_TRACE_ID.pattern());
        assertThat(traceId).isNotEqualTo("trace\nwith-invalid-value");
        assertThat(MDC.get(HttpRequestLoggingFilter.TRACE_ID_MDC_KEY)).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "200, INFO",
            "400, WARN",
            "500, ERROR"
    })
    @DisplayName("응답 상태 코드에 맞는 레벨로 요청을 한 번 기록한다")
    void logsRequestOnceWithLevelBasedOnResponseStatus(int status, String expectedLevel) throws Exception {
        String traceId = "trace-123";
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/users");
        request.addHeader(HttpRequestLoggingFilter.TRACE_ID_HEADER, traceId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (servletRequest, servletResponse) ->
                ((MockHttpServletResponse) servletResponse).setStatus(status);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent event = logAppender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.valueOf(expectedLevel));
        assertThat(event.getFormattedMessage())
                .contains("http_request method=POST path=/api/v1/users status=" + status)
                .contains("duration_ms=")
                .contains("trace_id=" + traceId);
    }
}

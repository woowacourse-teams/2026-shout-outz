package com.shoutoutz.api.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.sql.PreparedStatement;
import java.util.List;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

class SlowQueryLoggingListenerTest {

    private static final long THRESHOLD_MILLIS = 50;

    private final SlowQueryLoggingListener listener = new SlowQueryLoggingListener(THRESHOLD_MILLIS);
    private Logger logger;
    private ListAppender<ILoggingEvent> logAppender;
    private Level previousLevel;

    @BeforeEach
    void setUp() {
        MDC.clear();
        RequestContextHolder.resetRequestAttributes();
        logger = (Logger) LoggerFactory.getLogger(SlowQueryLoggingListener.class);
        previousLevel = logger.getLevel();
        logger.setLevel(Level.TRACE);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logger.setLevel(previousLevel);
        MDC.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("기준 시간을 넘은 쿼리는 요청의 traceId·route와 함께 한 줄로 남긴다")
    void logsQueryOverThresholdWithTraceIdAndRoute() {
        MDC.put(HttpRequestLoggingFilter.TRACE_ID_MDC_KEY, "trace-123");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/feeds/1");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/feeds/{feedId}");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        listener.afterQuery(executionInfo(51), List.of(new QueryInfo("select * from feeds where id = ?")));

        assertThat(logAppender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getFormattedMessage()).isEqualTo(
                    "slow_query duration_ms=51 trace_id=trace-123 route=/api/v1/feeds/{feedId} "
                            + "sql=select * from feeds where id = ?");
        });
    }

    @Test
    @DisplayName("기준 시간 이하인 쿼리는 남기지 않는다")
    void doesNotLogQueryAtOrUnderThreshold() {
        listener.afterQuery(executionInfo(THRESHOLD_MILLIS), List.of(new QueryInfo("select 1")));

        assertThat(logAppender.list).isEmpty();
    }

    @Test
    @DisplayName("SQL의 줄바꿈과 연속 공백은 공백 하나로 접고, 여러 쿼리는 이어 붙인다")
    void collapsesWhitespacesAndJoinsQueries() {
        List<QueryInfo> queries = List.of(
                new QueryInfo("""
                        select *
                            from feeds
                        where id = ?
                        """),
                new QueryInfo("select count(*)\n\tfrom comments"));

        listener.afterQuery(executionInfo(120), queries);

        assertThat(singleLogMessage())
                .endsWith("sql=select * from feeds where id = ?; select count(*) from comments");
    }

    @Test
    @DisplayName("파라미터 값은 남기지 않는다")
    void doesNotLogParameterValues() throws NoSuchMethodException {
        QueryInfo queryInfo = new QueryInfo("select * from users where handle = ?");
        queryInfo.getParametersList().add(List.of(new ParameterSetOperation(
                PreparedStatement.class.getMethod("setString", int.class, String.class),
                new Object[]{1, "secret-handle"})));

        listener.afterQuery(executionInfo(80), List.of(queryInfo));

        assertThat(singleLogMessage())
                .contains("where handle = ?")
                .doesNotContain("secret-handle");
    }

    @Test
    @DisplayName("요청 밖에서 나간 쿼리는 traceId와 route를 -로 남긴다")
    void logsDashWhenOutsideRequest() {
        listener.afterQuery(executionInfo(70), List.of(new QueryInfo("select 1")));

        assertThat(singleLogMessage()).isEqualTo("slow_query duration_ms=70 trace_id=- route=- sql=select 1");
    }

    private String singleLogMessage() {
        assertThat(logAppender.list).hasSize(1);
        return logAppender.list.getFirst().getFormattedMessage();
    }

    private ExecutionInfo executionInfo(long elapsedMillis) {
        ExecutionInfo executionInfo = new ExecutionInfo();
        executionInfo.setElapsedTime(elapsedMillis);
        return executionInfo;
    }
}

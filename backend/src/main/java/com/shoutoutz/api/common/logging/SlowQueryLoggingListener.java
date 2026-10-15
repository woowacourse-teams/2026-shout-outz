package com.shoutoutz.api.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 기준 시간을 넘은 쿼리를 요청 로그와 같은 key=value 한 줄로 남긴다.
 * 커넥션을 얻은 뒤 쿼리를 실행한 시간만 재므로, 커넥션 대기 시간은 포함되지 않는다.
 * 사용자 입력이 로그에 남지 않도록 파라미터 값은 기록하지 않는다.
 */
@Slf4j
public class SlowQueryLoggingListener implements QueryExecutionListener {

    private static final String NONE = "-";
    private static final String QUERY_DELIMITER = "; ";
    private static final Pattern WHITESPACES = Pattern.compile("\\s+");

    private final long thresholdMillis;

    public SlowQueryLoggingListener(long thresholdMillis) {
        this.thresholdMillis = thresholdMillis;
    }

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        long elapsedMillis = execInfo.getElapsedTime();
        if (elapsedMillis <= thresholdMillis) {
            return;
        }
        // 새 필드는 끝에 덧붙인다. (요청 로그와 같은 규칙)
        log.info(
                "slow_query duration_ms={} trace_id={} route={} sql={}",
                elapsedMillis,
                resolveTraceId(),
                resolveRoute(),
                toSingleLine(queryInfoList)
        );
    }

    private String resolveTraceId() {
        String traceId = MDC.get(HttpRequestLoggingFilter.TRACE_ID_MDC_KEY);
        if (traceId == null) {
            return NONE;
        }
        return traceId;
    }

    /**
     * 쿼리를 보낸 요청의 URL 템플릿을 반환한다.
     * 스케줄러처럼 요청 밖에서 나간 쿼리는 {@code -}를 반환한다.
     */
    private String resolveRoute() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return NONE;
        }
        HttpServletRequest request = servletAttributes.getRequest();
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern == null) {
            return NONE;
        }
        return pattern.toString();
    }

    private String toSingleLine(List<QueryInfo> queryInfoList) {
        return queryInfoList.stream()
                .map(queryInfo -> WHITESPACES.matcher(queryInfo.getQuery()).replaceAll(" ").strip())
                .collect(Collectors.joining(QUERY_DELIMITER));
    }
}

package com.shoutoutz.api.jobposting.application;

import com.shoutoutz.api.jobposting.infrastructure.JobRequirementMatchingProperties;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 실패한 역량을 언제 다시 가져갈지 정한다. 다시 시도할지는 Worker 가 시도 횟수와 실패 종류로 정한다.
 */
@Component
@ConditionalOnProperty(prefix = "job-posting.matching", name = "enabled", havingValue = "true")
public class JobRequirementMatchingRetryPolicy {

    private final List<Duration> retryDelays;

    public JobRequirementMatchingRetryPolicy(JobRequirementMatchingProperties properties) {
        this.retryDelays = properties.retryDelays();
    }

    /**
     * @param attemptCount 방금 실패한 시도의 횟수 (1부터)
     */
    public Duration delayAfter(int attemptCount) {
        int index = Math.min(Math.max(attemptCount, 1), retryDelays.size()) - 1;
        return retryDelays.get(index);
    }
}

package com.shoutoutz.api.jobposting.infrastructure;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 역량 매칭 Worker 설정. 실제 검색·판정을 연결하기 전까지 enabled 는 false 로 둔다.
 *
 * @param lease       역량을 가져간 뒤 다른 Worker 가 다시 가져가지 못하는 시간. 검색·판정 제한 시간의 합보다 길어야 한다
 * @param retryDelays n 번째 시도가 실패한 뒤 기다리는 시간. 시도가 목록보다 많으면 마지막 값을 쓴다
 * @param searchLimit 역량 하나당 판정에 넘길 최대 후보 자료 수
 */
@ConfigurationProperties(prefix = "job-posting.matching")
public record JobRequirementMatchingProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("10s") Duration pollInterval,
        @DefaultValue("180s") Duration lease,
        @DefaultValue("3") int maxAttempts,
        @DefaultValue({"1m", "5m"}) List<Duration> retryDelays,
        @DefaultValue("10") int searchLimit
) {

    public JobRequirementMatchingProperties {
        requirePositive(pollInterval, "poll-interval");
        requirePositive(lease, "lease");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("job-posting.matching.max-attempts는 1 이상이어야 합니다.");
        }
        if (retryDelays == null || retryDelays.isEmpty()) {
            throw new IllegalArgumentException("job-posting.matching.retry-delays를 하나 이상 설정해야 합니다.");
        }
        retryDelays.forEach(delay -> requirePositive(delay, "retry-delays"));
        retryDelays = List.copyOf(retryDelays);
        if (searchLimit < 1) {
            throw new IllegalArgumentException("job-posting.matching.search-limit는 1 이상이어야 합니다.");
        }
    }

    private static void requirePositive(Duration duration, String key) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("job-posting.matching." + key + "는 양수여야 합니다.");
        }
    }
}

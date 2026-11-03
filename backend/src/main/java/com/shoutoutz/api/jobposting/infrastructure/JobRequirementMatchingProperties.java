package com.shoutoutz.api.jobposting.infrastructure;

import com.shoutoutz.api.common.llm.LlmProvider;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 역량 매칭 Worker 설정. 기본은 꺼져 있고 환경마다 enabled 로 켠다.
 *
 * @param lease       역량을 가져간 뒤 다른 Worker 가 다시 가져가지 못하는 시간. 임베딩과 판정 제공자 제한 시간의 합보다 길어야 한다
 * @param retryDelays n 번째 시도가 실패한 뒤 기다리는 시간. 시도가 목록보다 많으면 마지막 값을 쓴다
 * @param searchLimit 역량 하나당 판정에 넘길 최대 후보 자료 수. 판정 응답이 출력 상한을 넘지 않도록 10개까지만 받는다
 * @param providers   판정 LLM 제공자. 앞에서부터 호출하고 실패하면 다음 제공자로 넘어간다
 */
@ConfigurationProperties(prefix = "job-posting.matching")
public record JobRequirementMatchingProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("10s") Duration pollInterval,
        @DefaultValue("180s") Duration lease,
        @DefaultValue("3") int maxAttempts,
        @DefaultValue({"1m", "5m"}) List<Duration> retryDelays,
        @DefaultValue("10") int searchLimit,
        @DefaultValue({"BEDROCK_GPT", "BEDROCK_CLAUDE"}) List<LlmProvider> providers
) {

    /**
     * 판정은 후보마다 이유와 설명을 한 응답에 담아, 후보가 늘면 판정 출력 상한을 넘어 응답이 잘릴 수 있다.
     * 늘리려면 LlmRequirementJudge 의 출력 상한도 함께 본다.
     */
    static final int MAX_SEARCH_LIMIT = 10;

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
        if (searchLimit < 1 || searchLimit > MAX_SEARCH_LIMIT) {
            throw new IllegalArgumentException(
                    "job-posting.matching.search-limit는 1 이상 " + MAX_SEARCH_LIMIT + " 이하여야 합니다.");
        }
        if (providers == null || providers.isEmpty()) {
            throw new IllegalArgumentException("job-posting.matching.providers를 하나 이상 설정해야 합니다.");
        }
        if (new HashSet<>(providers).size() != providers.size()) {
            throw new IllegalArgumentException("job-posting.matching.providers에 같은 제공자를 두 번 넣을 수 없습니다.");
        }
        providers = List.copyOf(providers);
    }

    private static void requirePositive(Duration duration, String key) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("job-posting.matching." + key + "는 양수여야 합니다.");
        }
    }
}

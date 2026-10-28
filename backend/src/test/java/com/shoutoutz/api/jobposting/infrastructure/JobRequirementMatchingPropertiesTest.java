package com.shoutoutz.api.jobposting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class JobRequirementMatchingPropertiesTest {

    @Test
    void 설정하지_않으면_꺼진_채로_결정한_기본값을_쓴다() {
        JobRequirementMatchingProperties properties = bind(Map.of());

        assertThat(properties.enabled()).isFalse();
        assertThat(properties.pollInterval()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.lease()).isEqualTo(Duration.ofSeconds(180));
        assertThat(properties.maxAttempts()).isEqualTo(3);
        assertThat(properties.retryDelays()).containsExactly(Duration.ofMinutes(1), Duration.ofMinutes(5));
        assertThat(properties.searchLimit()).isEqualTo(10);
    }

    @Test
    void yml로_값을_바꿀_수_있다() {
        JobRequirementMatchingProperties properties = bind(Map.of(
                "job-posting.matching.enabled", "true",
                "job-posting.matching.max-attempts", "5",
                "job-posting.matching.retry-delays", "30s,2m,10m",
                "job-posting.matching.search-limit", "5"
        ));

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.maxAttempts()).isEqualTo(5);
        assertThat(properties.retryDelays())
                .containsExactly(Duration.ofSeconds(30), Duration.ofMinutes(2), Duration.ofMinutes(10));
        assertThat(properties.searchLimit()).isEqualTo(5);
    }

    @Test
    void 시도_횟수가_1보다_작으면_실패한다() {
        assertThatThrownBy(() -> properties(0, List.of(Duration.ofMinutes(1)), 10))
                .hasMessageContaining("max-attempts");
    }

    @Test
    void 재시도_대기_시간이_없거나_양수가_아니면_실패한다() {
        assertThatThrownBy(() -> properties(3, List.of(), 10))
                .hasMessageContaining("retry-delays");
        assertThatThrownBy(() -> properties(3, List.of(Duration.ZERO), 10))
                .hasMessageContaining("retry-delays");
    }

    @Test
    void 후보_자료_수가_1보다_작으면_실패한다() {
        assertThatThrownBy(() -> properties(3, List.of(Duration.ofMinutes(1)), 0))
                .hasMessageContaining("search-limit");
    }

    private JobRequirementMatchingProperties properties(int maxAttempts, List<Duration> retryDelays, int searchLimit) {
        return new JobRequirementMatchingProperties(
                true, Duration.ofSeconds(10), Duration.ofSeconds(180), maxAttempts, retryDelays, searchLimit);
    }

    private JobRequirementMatchingProperties bind(Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values))
                .bindOrCreate("job-posting.matching", JobRequirementMatchingProperties.class);
    }
}

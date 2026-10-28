package com.shoutoutz.api.jobposting.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.jobposting.infrastructure.JobRequirementMatchingProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class JobRequirementMatchingRetryPolicyTest {

    private final JobRequirementMatchingRetryPolicy policy = new JobRequirementMatchingRetryPolicy(
            new JobRequirementMatchingProperties(
                    true,
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(180),
                    3,
                    List.of(Duration.ofMinutes(1), Duration.ofMinutes(5)),
                    10
            )
    );

    @Test
    void 첫_시도가_실패하면_1분_두_번째_시도가_실패하면_5분_기다린다() {
        assertThat(policy.delayAfter(1)).isEqualTo(Duration.ofMinutes(1));
        assertThat(policy.delayAfter(2)).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void 시도가_설정한_대기_시간보다_많으면_마지막_값을_쓴다() {
        assertThat(policy.delayAfter(3)).isEqualTo(Duration.ofMinutes(5));
        assertThat(policy.delayAfter(Integer.MAX_VALUE)).isEqualTo(Duration.ofMinutes(5));
    }
}

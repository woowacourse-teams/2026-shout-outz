package com.shoutoutz.api.jobposting.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MatchingFailureTest {

    @ParameterizedTest
    @EnumSource(value = MatchingFailure.class, names = {"SEARCH_FAILED", "JUDGE_FAILED"})
    void 외부_장애는_다시_시도한다(MatchingFailure failure) {
        assertThat(failure.retryable()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = MatchingFailure.class, names = {"INVALID_QUERY", "UNEXPECTED"})
    void 다시_해도_같은_실패는_다시_시도하지_않는다(MatchingFailure failure) {
        assertThat(failure.retryable()).isFalse();
    }

    @Test
    void 이름이_last_error_칸_길이_안에_들어간다() {
        for (MatchingFailure failure : MatchingFailure.values()) {
            assertThat(failure.name().length()).isLessThanOrEqualTo(100);
        }
    }
}

package com.shoutoutz.api.jobposting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.jobposting.domain.MatchingFailure;
import org.junit.jupiter.api.Test;

class JobRequirementMatchingExceptionTest {

    @Test
    void 실패_분류와_원인을_함께_넘긴다() {
        IllegalStateException cause = new IllegalStateException("임베딩 호출 실패");

        JobRequirementMatchingException exception =
                new JobRequirementMatchingException(MatchingFailure.SEARCH_FAILED, cause);

        assertThat(exception.failure()).isEqualTo(MatchingFailure.SEARCH_FAILED);
        assertThat(exception).hasCause(cause);
    }

    @Test
    void 실패_분류_없이는_만들_수_없다() {
        assertThatThrownBy(() -> new JobRequirementMatchingException(null, new IllegalStateException()))
                .isInstanceOf(NullPointerException.class);
    }
}

package com.shoutoutz.api.cohort.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CohortTest {

    @Test
    @DisplayName("기수 번호로 기수와 진행 연도를 찾는다.")
    void findsCohortByValue() {
        Cohort cohort = Cohort.from(6);

        assertThat(cohort).isEqualTo(Cohort.COHORT_6);
        assertThat(cohort.getValue()).isEqualTo(6);
        assertThat(cohort.getYear()).isEqualTo(2024);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 9, -1})
    @DisplayName("정의되지 않은 기수 번호는 400 예외를 던진다.")
    void rejectsUndefinedCohort(int value) {
        assertThatThrownBy(() -> Cohort.from(value))
                .isInstanceOfSatisfying(BadRequestException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(CohortErrorCode.INVALID_COHORT));
    }

    @Test
    @DisplayName("최신 기수부터 내림차순으로 반환한다.")
    void returnsCohortsInDescendingOrder() {
        List<Cohort> cohorts = Cohort.descending();

        assertThat(cohorts).hasSize(8);
        assertThat(cohorts.getFirst()).isEqualTo(Cohort.COHORT_8);
        assertThat(cohorts.getLast()).isEqualTo(Cohort.COHORT_1);
    }
}

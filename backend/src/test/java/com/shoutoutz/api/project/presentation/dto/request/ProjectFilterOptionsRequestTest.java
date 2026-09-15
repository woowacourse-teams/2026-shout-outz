package com.shoutoutz.api.project.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.cohort.domain.CohortErrorCode;
import com.shoutoutz.api.cohort.domain.InvalidCohortException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProjectFilterOptionsRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("파라미터를 입력하지 않으면 검증을 통과하고, 검색어와 필터는 적용하지 않는다.")
    void usesDefaultsWhenAbsent() {
        ProjectFilterOptionsRequest request = new ProjectFilterOptionsRequest(null, null, null);

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.keyword()).isNull();
        assertThat(request.resolvedCohorts()).isEmpty();
        assertThat(request.resolvedTechTagIds()).isEmpty();
    }

    @Test
    @DisplayName("검색어의 앞뒤 공백을 제거한다.")
    void trimsKeyword() {
        assertThat(keyword(" 모아모아 ").keyword()).isEqualTo("모아모아");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    @DisplayName("공백뿐인 검색어는 검색하지 않는다.")
    void ignoresBlankKeyword(String keyword) {
        assertThat(keyword(keyword).keyword()).isNull();
    }

    @Test
    @DisplayName("검색어가 코드 포인트 기준 100자를 넘으면 검증에 실패한다.")
    void rejectsTooLongKeyword() {
        assertThat(violatedProperties(keyword("😀".repeat(100)))).doesNotContain("keyword");
        assertThat(violatedProperties(keyword("😀".repeat(101)))).contains("keyword");
    }

    @Test
    @DisplayName("기수와 기술 스택 필터는 입력 순서를 유지한 채 중복을 제거한다.")
    void removesDuplicateFilters() {
        ProjectFilterOptionsRequest request = new ProjectFilterOptionsRequest(
                null, List.of(7, 6, 7), List.of(3L, 1L, 3L));

        assertThat(request.resolvedCohorts()).containsExactly(7, 6);
        assertThat(request.resolvedTechTagIds()).containsExactly(3L, 1L);
    }

    @Test
    @DisplayName("필터에 빈 값이 섞여 있으면 검증에 실패한다.")
    void rejectsNullFilterElements() {
        ProjectFilterOptionsRequest request = new ProjectFilterOptionsRequest(
                null, Arrays.asList(6, null), Arrays.asList(1L, null));

        assertThat(violatedProperties(request))
                .anyMatch(property -> property.startsWith("cohorts"))
                .anyMatch(property -> property.startsWith("techTagIds"));
    }

    @Test
    @DisplayName("정의되지 않은 기수를 선택하면 400을 던진다.")
    void rejectsUndefinedCohort() {
        ProjectFilterOptionsRequest request = new ProjectFilterOptionsRequest(null, List.of(6, 99), null);

        assertThatThrownBy(request::resolvedCohorts)
                .isInstanceOfSatisfying(InvalidCohortException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(CohortErrorCode.INVALID_COHORT));
    }

    private static ProjectFilterOptionsRequest keyword(String keyword) {
        return new ProjectFilterOptionsRequest(keyword, null, null);
    }

    private List<String> violatedProperties(ProjectFilterOptionsRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .toList();
    }
}

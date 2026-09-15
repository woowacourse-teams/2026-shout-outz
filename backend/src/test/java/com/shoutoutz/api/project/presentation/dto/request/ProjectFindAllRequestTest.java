package com.shoutoutz.api.project.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.cohort.domain.CohortErrorCode;
import com.shoutoutz.api.cohort.domain.InvalidCohortException;
import com.shoutoutz.api.project.application.ProjectCursorCodec;
import com.shoutoutz.api.project.domain.ProjectCursor;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.domain.ProjectSort;
import com.shoutoutz.api.project.domain.exception.InvalidProjectCursorException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProjectFindAllRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("파라미터를 입력하지 않으면 검증을 통과하고, 최신순으로 8개를 조회하며 검색어와 필터는 적용하지 않는다.")
    void usesDefaultsWhenAbsent() {
        ProjectFindAllRequest request = new ProjectFindAllRequest(null, null, null, null, null, null);

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.keyword()).isNull();
        assertThat(request.resolvedCohorts()).isEmpty();
        assertThat(request.resolvedTechTagIds()).isEmpty();
        assertThat(request.resolvedSort()).isEqualTo(ProjectSort.LATEST);
        assertThat(request.resolvedSize()).isEqualTo(8);
        assertThat(request.cursor()).isNull();
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
    @DisplayName("검색어는 코드 포인트 기준 100자까지 입력할 수 있다.")
    void acceptsKeywordUpToMaxLength() {
        assertThat(violatedProperties(keyword("😀".repeat(100)))).doesNotContain("keyword");
    }

    @Test
    @DisplayName("검색어가 코드 포인트 기준 100자를 넘으면 검증에 실패한다.")
    void rejectsTooLongKeyword() {
        assertThat(violatedProperties(keyword("😀".repeat(101)))).contains("keyword");
    }

    @Test
    @DisplayName("POPULAR 정렬을 입력하면 인기순으로 조회한다.")
    void resolvesPopularSort() {
        ProjectFindAllRequest request = new ProjectFindAllRequest(null, null, null, "POPULAR", null, null);

        assertThat(violatedProperties(request)).isEmpty();
        assertThat(request.resolvedSort()).isEqualTo(ProjectSort.POPULAR);
    }

    @ParameterizedTest
    @ValueSource(strings = {"popular", "OLDEST"})
    @DisplayName("정의되지 않은 정렬 기준이면 검증에 실패한다.")
    void rejectsUnknownSort(String sort) {
        ProjectFindAllRequest request = new ProjectFindAllRequest(null, null, null, sort, null, null);

        assertThat(violatedProperties(request)).contains("sort");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 50})
    @DisplayName("조회 개수는 1 이상 50 이하로 입력할 수 있다.")
    void acceptsSizeInRange(int size) {
        ProjectFindAllRequest request = new ProjectFindAllRequest(null, null, null, null, size, null);

        assertThat(violatedProperties(request)).isEmpty();
        assertThat(request.resolvedSize()).isEqualTo(size);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 51})
    @DisplayName("조회 개수가 1 미만이거나 50을 넘으면 검증에 실패한다.")
    void rejectsSizeOutOfRange(int size) {
        ProjectFindAllRequest request = new ProjectFindAllRequest(null, null, null, null, size, null);

        assertThat(violatedProperties(request)).contains("size");
    }

    @Test
    @DisplayName("기수와 기술 스택 필터는 입력 순서를 유지한 채 중복을 제거한다.")
    void removesDuplicateFilters() {
        ProjectFindAllRequest request = new ProjectFindAllRequest(
                null, List.of(7, 6, 7), List.of(3L, 1L, 3L), null, null, null);

        assertThat(request.resolvedCohorts()).containsExactly(7, 6);
        assertThat(request.resolvedTechTagIds()).containsExactly(3L, 1L);
    }

    @Test
    @DisplayName("필터에 빈 값이 섞여 있으면 검증에 실패한다.")
    void rejectsNullFilterElements() {
        ProjectFindAllRequest request = new ProjectFindAllRequest(
                null, Arrays.asList(6, null), Arrays.asList(1L, null), null, null, null);

        assertThat(violatedProperties(request))
                .anyMatch(property -> property.startsWith("cohorts"))
                .anyMatch(property -> property.startsWith("techTagIds"));
    }

    @Test
    @DisplayName("정의되지 않은 기수로 필터링하면 400을 던진다.")
    void rejectsUndefinedCohort() {
        ProjectFindAllRequest request = new ProjectFindAllRequest(null, List.of(6, 99), null, null, null, null);

        assertThatThrownBy(request::resolvedCohorts)
                .isInstanceOfSatisfying(InvalidCohortException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(CohortErrorCode.INVALID_COHORT));
    }

    @Test
    @DisplayName("공백뿐인 커서는 첫 페이지 조회로 본다.")
    void ignoresBlankCursor() {
        ProjectFindAllRequest request = new ProjectFindAllRequest(null, null, null, null, null, " ");

        assertThat(request.cursor()).isNull();
    }

    @Test
    @DisplayName("커서를 입력하지 않으면 첫 페이지를 조회한다.")
    void resolvesNullCursorForFirstPage() {
        ProjectFindAllRequest request = new ProjectFindAllRequest(null, null, null, null, null, null);

        assertThat(request.resolvedCursor()).isNull();
    }

    @Test
    @DisplayName("요청한 정렬 기준으로 커서를 해독한다.")
    void resolvesCursorWithRequestedSort() {
        ProjectCursor cursor = ProjectCursor.popular(84L, Instant.parse("2026-08-09T02:30:00Z"), 100L);
        ProjectFindAllRequest request = new ProjectFindAllRequest(
                null, null, null, "POPULAR", null, ProjectCursorCodec.encode(cursor));

        assertThat(request.resolvedCursor()).isEqualTo(cursor);
    }

    @Test
    @DisplayName("정렬을 바꾸고 이전 정렬의 커서를 보내면 400을 던진다.")
    void rejectsCursorOfDifferentSort() {
        String latestCursor = ProjectCursorCodec.encode(
                ProjectCursor.latest(Instant.parse("2026-08-09T02:30:00Z"), 100L));
        ProjectFindAllRequest request = new ProjectFindAllRequest(null, null, null, "POPULAR", null, latestCursor);

        assertThatThrownBy(request::resolvedCursor)
                .isInstanceOfSatisfying(InvalidProjectCursorException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_INVALID_CURSOR));
    }

    private static ProjectFindAllRequest keyword(String keyword) {
        return new ProjectFindAllRequest(keyword, null, null, null, null, null);
    }

    private List<String> violatedProperties(ProjectFindAllRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .toList();
    }
}

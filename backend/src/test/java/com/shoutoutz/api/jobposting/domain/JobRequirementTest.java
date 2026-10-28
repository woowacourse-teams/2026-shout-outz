package com.shoutoutz.api.jobposting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class JobRequirementTest {

    @Test
    @DisplayName("새 역량 항목은 앞뒤 공백을 정리하고 PENDING 상태로 만든다.")
    void createsPendingRequirement() {
        JobRequirement requirement = JobRequirement.create(
                1, "  대용량 트래픽 처리  ", RequirementCategory.PREFERRED, " 트래픽이 많은 서비스를 운영해 본 경험 ");

        assertThat(requirement.getId()).isNull();
        assertThat(requirement.getDisplayOrder()).isEqualTo(1);
        assertThat(requirement.getName()).isEqualTo("대용량 트래픽 처리");
        assertThat(requirement.getCategory()).isEqualTo(RequirementCategory.PREFERRED);
        assertThat(requirement.getSummary()).isEqualTo("트래픽이 많은 서비스를 운영해 본 경험");
        assertThat(requirement.getStatus()).isEqualTo(MatchingStatus.PENDING);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 51})
    @DisplayName("보여 줄 순서가 1~50 밖이면 도메인 예외를 던진다.")
    void rejectsInvalidDisplayOrder(int displayOrder) {
        assertErrorCode(
                () -> JobRequirement.create(displayOrder, "Java", RequirementCategory.REQUIRED, "Java 개발 경험"),
                JobPostingErrorCode.JOB_POSTING_INVALID_REQUIREMENT_ORDER);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("역량 이름이 비어 있으면 도메인 예외를 던진다.")
    void rejectsBlankName(String name) {
        assertErrorCode(
                () -> JobRequirement.create(1, name, RequirementCategory.REQUIRED, "Java 개발 경험"),
                JobPostingErrorCode.JOB_REQUIREMENT_NAME_NULL_OR_BLANK);
    }

    @Test
    @DisplayName("역량 이름이 100자를 넘으면 도메인 예외를 던진다.")
    void rejectsTooLongName() {
        assertErrorCode(
                () -> JobRequirement.create(1, "가".repeat(101), RequirementCategory.REQUIRED, "Java 개발 경험"),
                JobPostingErrorCode.JOB_REQUIREMENT_INVALID_NAME_LENGTH);
    }

    @Test
    @DisplayName("역량 구분이 없으면 도메인 예외를 던진다.")
    void rejectsNullCategory() {
        assertErrorCode(
                () -> JobRequirement.create(1, "Java", null, "Java 개발 경험"),
                JobPostingErrorCode.JOB_REQUIREMENT_CATEGORY_NULL);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("역량 요약이 비어 있으면 도메인 예외를 던진다.")
    void rejectsBlankSummary(String summary) {
        assertErrorCode(
                () -> JobRequirement.create(1, "Java", RequirementCategory.REQUIRED, summary),
                JobPostingErrorCode.JOB_REQUIREMENT_SUMMARY_NULL_OR_BLANK);
    }

    @Test
    @DisplayName("역량 요약이 500자를 넘으면 도메인 예외를 던진다.")
    void rejectsTooLongSummary() {
        assertErrorCode(
                () -> JobRequirement.create(1, "Java", RequirementCategory.REQUIRED, "가".repeat(501)),
                JobPostingErrorCode.JOB_REQUIREMENT_INVALID_SUMMARY_LENGTH);
    }

    @Test
    @DisplayName("다시 만들 때 매칭 상태가 없으면 도메인 예외를 던진다.")
    void rejectsNullStatusOnReconstitute() {
        assertErrorCode(
                () -> JobRequirement.reconstitute(1L, 1, "Java", RequirementCategory.REQUIRED, "Java 개발 경험", null),
                JobPostingErrorCode.JOB_REQUIREMENT_STATUS_NULL);
    }

    private void assertErrorCode(ThrowingCallable callable, ErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(expected));
    }
}

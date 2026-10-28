package com.shoutoutz.api.jobposting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class JobPostingTest {

    private static final String URL = "https://Careers.Example.com/jobs/123/";
    private static final String CONTENT = "자격 요건\n- Java 개발 경험";
    private static final Instant NOW = Instant.parse("2026-10-27T01:00:00Z");

    @Test
    @DisplayName("새 공고는 주소를 정규화하고, 원문은 해시만 남기며, PENDING 상태로 만든다.")
    void registersPendingPosting() {
        JobPosting posting = JobPosting.register(
                URL, CONTENT, "  예시 회사 ", JobRole.BACKEND, " 3년 이상 ", " 백엔드 개발자 ", " 정규직 ",
                LocalDate.of(2026, 11, 30), 1L, requirements(2), NOW);

        assertThat(posting.getId()).isNull();
        assertThat(posting.getSourceUrl().value()).isEqualTo("https://careers.example.com/jobs/123");
        assertThat(posting.getContentHash()).isEqualTo(JobPostingContentHash.from(CONTENT));
        assertThat(posting.getCompanyName()).isEqualTo("예시 회사");
        assertThat(posting.getCareerRequirement()).isEqualTo("3년 이상");
        assertThat(posting.getTitle()).isEqualTo("백엔드 개발자");
        assertThat(posting.getEmploymentType()).isEqualTo("정규직");
        assertThat(posting.getStatus()).isEqualTo(MatchingStatus.PENDING);
        assertThat(posting.getHiddenAt()).isNull();
        assertThat(posting.getRequirements()).hasSize(2);
        assertThat(posting.getCreatedAt()).isEqualTo(NOW);
        assertThat(posting.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("공고에 없는 경력 조건, 고용 형태, 마감일은 비워 둘 수 있다.")
    void allowsMissingOptionalFields() {
        JobPosting posting = JobPosting.register(
                URL, CONTENT, "예시 회사", JobRole.BACKEND, "  ", "백엔드 개발자", null,
                null, 1L, requirements(1), NOW);

        assertThat(posting.getCareerRequirement()).isNull();
        assertThat(posting.getEmploymentType()).isNull();
        assertThat(posting.getDeadline()).isNull();
    }

    @Test
    @DisplayName("역량 목록은 밖에서 바꿔도 공고 안의 목록이 바뀌지 않는다.")
    void copiesRequirements() {
        List<JobRequirement> requirements = new ArrayList<>(requirements(1));
        JobPosting posting = register(requirements);

        requirements.add(JobRequirement.create(2, "Kotlin", RequirementCategory.PREFERRED, "Kotlin 개발 경험"));

        assertThat(posting.getRequirements()).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 51})
    @DisplayName("역량 항목이 없거나 50개를 넘으면 도메인 예외를 던진다.")
    void rejectsInvalidRequirementCount(int count) {
        assertErrorCode(() -> register(requirements(count)), JobPostingErrorCode.JOB_POSTING_INVALID_REQUIREMENT_COUNT);
    }

    @Test
    @DisplayName("역량 목록이 null 이면 도메인 예외를 던진다.")
    void rejectsNullRequirements() {
        assertErrorCode(() -> register(null), JobPostingErrorCode.JOB_POSTING_INVALID_REQUIREMENT_COUNT);
    }

    @Test
    @DisplayName("역량 항목의 순서가 1부터 이어지지 않으면 도메인 예외를 던진다.")
    void rejectsBrokenRequirementOrder() {
        List<JobRequirement> requirements = List.of(
                JobRequirement.create(1, "Java", RequirementCategory.REQUIRED, "Java 개발 경험"),
                JobRequirement.create(3, "Kotlin", RequirementCategory.PREFERRED, "Kotlin 개발 경험"));

        assertErrorCode(() -> register(requirements), JobPostingErrorCode.JOB_POSTING_INVALID_REQUIREMENT_ORDER);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("회사명이 비어 있으면 도메인 예외를 던진다.")
    void rejectsBlankCompanyName(String companyName) {
        assertErrorCode(
                () -> JobPosting.register(URL, CONTENT, companyName, JobRole.BACKEND, null, "백엔드 개발자", null,
                        null, 1L, requirements(1), NOW),
                JobPostingErrorCode.JOB_POSTING_COMPANY_NAME_NULL_OR_BLANK);
    }

    @Test
    @DisplayName("회사명이 100자를 넘으면 도메인 예외를 던진다.")
    void rejectsTooLongCompanyName() {
        assertErrorCode(
                () -> JobPosting.register(URL, CONTENT, "가".repeat(101), JobRole.BACKEND, null, "백엔드 개발자", null,
                        null, 1L, requirements(1), NOW),
                JobPostingErrorCode.JOB_POSTING_INVALID_COMPANY_NAME_LENGTH);
    }

    @Test
    @DisplayName("직무가 없으면 도메인 예외를 던진다.")
    void rejectsNullJobRole() {
        assertErrorCode(
                () -> JobPosting.register(URL, CONTENT, "예시 회사", null, null, "백엔드 개발자", null,
                        null, 1L, requirements(1), NOW),
                JobPostingErrorCode.JOB_POSTING_JOB_ROLE_NULL);
    }

    @Test
    @DisplayName("경력 조건이 50자를 넘으면 도메인 예외를 던진다.")
    void rejectsTooLongCareerRequirement() {
        assertErrorCode(
                () -> JobPosting.register(URL, CONTENT, "예시 회사", JobRole.BACKEND, "가".repeat(51), "백엔드 개발자", null,
                        null, 1L, requirements(1), NOW),
                JobPostingErrorCode.JOB_POSTING_INVALID_CAREER_REQUIREMENT);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("공고 제목이 비어 있으면 도메인 예외를 던진다.")
    void rejectsBlankTitle(String title) {
        assertErrorCode(
                () -> JobPosting.register(URL, CONTENT, "예시 회사", JobRole.BACKEND, null, title, null,
                        null, 1L, requirements(1), NOW),
                JobPostingErrorCode.JOB_POSTING_TITLE_NULL_OR_BLANK);
    }

    @Test
    @DisplayName("공고 제목이 200자를 넘으면 도메인 예외를 던진다.")
    void rejectsTooLongTitle() {
        assertErrorCode(
                () -> JobPosting.register(URL, CONTENT, "예시 회사", JobRole.BACKEND, null, "가".repeat(201), null,
                        null, 1L, requirements(1), NOW),
                JobPostingErrorCode.JOB_POSTING_INVALID_TITLE_LENGTH);
    }

    @Test
    @DisplayName("고용 형태가 30자를 넘으면 도메인 예외를 던진다.")
    void rejectsTooLongEmploymentType() {
        assertErrorCode(
                () -> JobPosting.register(URL, CONTENT, "예시 회사", JobRole.BACKEND, null, "백엔드 개발자", "가".repeat(31),
                        null, 1L, requirements(1), NOW),
                JobPostingErrorCode.JOB_POSTING_INVALID_EMPLOYMENT_TYPE);
    }

    @Test
    @DisplayName("등록자 ID가 0 이하이면 도메인 예외를 던진다.")
    void rejectsInvalidRegisteredBy() {
        assertErrorCode(
                () -> JobPosting.register(URL, CONTENT, "예시 회사", JobRole.BACKEND, null, "백엔드 개발자", null,
                        null, 0L, requirements(1), NOW),
                JobPostingErrorCode.JOB_POSTING_INVALID_REGISTERED_BY_SIZE);
    }

    @Test
    @DisplayName("등록 시각이 없으면 도메인 예외를 던진다.")
    void rejectsNullNow() {
        assertErrorCode(
                () -> JobPosting.register(URL, CONTENT, "예시 회사", JobRole.BACKEND, null, "백엔드 개발자", null,
                        null, 1L, requirements(1), null),
                JobPostingErrorCode.JOB_POSTING_CREATED_AT_NULL);
    }

    @Test
    @DisplayName("저장된 값으로 다시 만들면 상태와 숨김 시각을 그대로 갖는다.")
    void reconstitutes() {
        String hash = JobPostingContentHash.from(CONTENT).value();
        Instant hiddenAt = NOW.plusSeconds(60);

        JobPosting posting = JobPosting.reconstitute(
                10L, "https://careers.example.com/jobs/123", hash, "예시 회사", JobRole.BACKEND, null,
                "백엔드 개발자", null, null, MatchingStatus.READY, hiddenAt, 1L, requirements(1), NOW, NOW);

        assertThat(posting.getId()).isEqualTo(10L);
        assertThat(posting.getContentHash().value()).isEqualTo(hash);
        assertThat(posting.getStatus()).isEqualTo(MatchingStatus.READY);
        assertThat(posting.getHiddenAt()).isEqualTo(hiddenAt);
    }

    @Test
    @DisplayName("다시 만들 때 매칭 상태가 없으면 도메인 예외를 던진다.")
    void rejectsNullStatusOnReconstitute() {
        String hash = JobPostingContentHash.from(CONTENT).value();

        assertErrorCode(
                () -> JobPosting.reconstitute(
                        10L, "https://careers.example.com/jobs/123", hash, "예시 회사", JobRole.BACKEND, null,
                        "백엔드 개발자", null, null, null, null, 1L, requirements(1), NOW, NOW),
                JobPostingErrorCode.JOB_POSTING_STATUS_NULL);
    }

    private JobPosting register(List<JobRequirement> requirements) {
        return JobPosting.register(
                URL, CONTENT, "예시 회사", JobRole.BACKEND, null, "백엔드 개발자", null,
                null, 1L, requirements, NOW);
    }

    private List<JobRequirement> requirements(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(order -> JobRequirement.create(
                        Math.min(order, 50), "역량 " + order, RequirementCategory.REQUIRED, "역량 요약 " + order))
                .toList();
    }

    private void assertErrorCode(ThrowingCallable callable, ErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(expected));
    }
}

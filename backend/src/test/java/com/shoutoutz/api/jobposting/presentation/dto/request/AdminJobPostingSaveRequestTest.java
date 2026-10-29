package com.shoutoutz.api.jobposting.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AdminJobPostingSaveRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("올바른 요청은 통과한다.")
    void acceptsValidRequest() {
        assertThat(validator.validate(request(List.of(requirement("Spring", "설명"))))).isEmpty();
    }

    @Test
    @DisplayName("문자열의 앞뒤 공백을 자르고, 비어 있는 선택값은 null 로 둔다.")
    void sanitizesStrings() {
        AdminJobPostingSaveRequest request = new AdminJobPostingSaveRequest(
                "  https://example.com/jobs/1  ", "  원문  ", "  회사  ", JobRole.BACKEND,
                "   ", " 백엔드 개발자 ", "", null,
                List.of(new AdminJobPostingSaveRequest.Requirement("  Spring ", RequirementCategory.REQUIRED, " 설명 "))
        );

        assertThat(request.sourceUrl()).isEqualTo("https://example.com/jobs/1");
        assertThat(request.content()).isEqualTo("  원문  ");
        assertThat(request.companyName()).isEqualTo("회사");
        assertThat(request.careerRequirement()).isNull();
        assertThat(request.title()).isEqualTo("백엔드 개발자");
        assertThat(request.employmentType()).isNull();
        assertThat(request.requirements().getFirst().name()).isEqualTo("Spring");
        assertThat(request.requirements().getFirst().summary()).isEqualTo("설명");
    }

    @Test
    @DisplayName("http 나 https 가 아닌 주소는 거절한다.")
    void rejectsInvalidUrl() {
        AdminJobPostingSaveRequest request = new AdminJobPostingSaveRequest(
                "ftp://example.com", "원문", "회사", JobRole.BACKEND, null, "제목", null, null,
                List.of(requirement("Spring", "설명"))
        );

        assertThat(violatedPaths(request)).containsExactly("sourceUrl");
    }

    @Test
    @DisplayName("공고 원문은 Unicode code point 기준으로 10,000자까지 허용한다.")
    void acceptsContentUpToMaxLength() {
        AdminJobPostingSaveRequest request = withContent("😀".repeat(AdminJobPostingSaveRequest.MAX_CONTENT_LENGTH));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("공고 원문이 10,000자를 초과하면 거절한다.")
    void rejectsContentOverMaxLength() {
        AdminJobPostingSaveRequest request = withContent("😀".repeat(AdminJobPostingSaveRequest.MAX_CONTENT_LENGTH + 1));

        assertThat(violatedPaths(request)).containsExactly("content");
    }

    @Test
    @DisplayName("필수 값이 없으면 거절한다.")
    void rejectsMissingRequiredFields() {
        AdminJobPostingSaveRequest request = new AdminJobPostingSaveRequest(
                null, " ", null, null, null, null, null, null, List.of(requirement("Spring", "설명"))
        );

        assertThat(violatedPaths(request))
                .containsExactlyInAnyOrder("sourceUrl", "content", "companyName", "jobRole", "title");
    }

    @Test
    @DisplayName("DB 칸 길이를 넘는 공고 정보는 거절한다.")
    void rejectsTooLongFields() {
        AdminJobPostingSaveRequest request = new AdminJobPostingSaveRequest(
                "https://example.com/" + "a".repeat(1_005), "원문", "가".repeat(101), JobRole.BACKEND,
                "가".repeat(51), "가".repeat(201), "가".repeat(31), LocalDate.of(2026, 12, 31),
                List.of(requirement("Spring", "설명"))
        );

        assertThat(violatedPaths(request)).containsExactlyInAnyOrder(
                "sourceUrl", "companyName", "careerRequirement", "title", "employmentType");
    }

    @Test
    @DisplayName("역량이 없으면 거절한다.")
    void rejectsEmptyRequirements() {
        assertThat(violatedPaths(request(List.of()))).containsExactly("requirements");
    }

    @Test
    @DisplayName("역량은 50개까지 허용하고, 넘으면 거절한다.")
    void rejectsTooManyRequirements() {
        List<AdminJobPostingSaveRequest.Requirement> fifty = Collections.nCopies(50, requirement("Spring", "설명"));
        List<AdminJobPostingSaveRequest.Requirement> fiftyOne = new ArrayList<>(fifty);
        fiftyOne.add(requirement("Spring", "설명"));

        assertThat(validator.validate(request(fifty))).isEmpty();
        assertThat(violatedPaths(request(fiftyOne))).containsExactly("requirements");
    }

    @Test
    @DisplayName("역량 목록에 null 이 있으면 거절한다.")
    void rejectsNullRequirement() {
        List<AdminJobPostingSaveRequest.Requirement> requirements = new ArrayList<>();
        requirements.add(null);

        assertThat(violatedPaths(request(requirements))).containsExactly("requirements[0].<list element>");
    }

    @Test
    @DisplayName("역량의 이름, 분류, 설명이 없거나 너무 길면 거절한다.")
    void rejectsInvalidRequirement() {
        AdminJobPostingSaveRequest request = request(List.of(
                new AdminJobPostingSaveRequest.Requirement(" ", null, "가".repeat(501)),
                requirement("가".repeat(101), "설명")
        ));

        assertThat(violatedPaths(request)).containsExactlyInAnyOrder(
                "requirements[0].name", "requirements[0].category", "requirements[0].summary",
                "requirements[1].name");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\u3000\u3000", "\u00A0", "\u2003\n"})
    @DisplayName("전각 공백 같은 유니코드 공백만 있는 필수 값은 400 으로 거절한다.")
    void rejectsUnicodeWhitespaceOnlyValues(String whitespace) {
        AdminJobPostingSaveRequest request = new AdminJobPostingSaveRequest(
                "https://example.com/jobs/1", whitespace, whitespace, JobRole.BACKEND, null, whitespace, null, null,
                List.of(new AdminJobPostingSaveRequest.Requirement(whitespace, RequirementCategory.REQUIRED, whitespace))
        );

        assertThat(violatedPaths(request)).containsExactlyInAnyOrder(
                "content", "companyName", "title", "requirements[0].name", "requirements[0].summary");
    }

    @Test
    @DisplayName("유니코드 공백이 섞여 있어도 글자가 있으면 통과한다.")
    void acceptsTextWithUnicodeWhitespace() {
        AdminJobPostingSaveRequest request = withContent("\u3000백엔드\u3000개발자\n");

        assertThat(validator.validate(request)).isEmpty();
    }

    private List<String> violatedPaths(AdminJobPostingSaveRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .toList();
    }

    private AdminJobPostingSaveRequest withContent(String content) {
        return new AdminJobPostingSaveRequest(
                "https://example.com/jobs/1", content, "회사", JobRole.BACKEND, null, "제목", null, null,
                List.of(requirement("Spring", "설명"))
        );
    }

    private AdminJobPostingSaveRequest request(List<AdminJobPostingSaveRequest.Requirement> requirements) {
        return new AdminJobPostingSaveRequest(
                "https://example.com/jobs/1", "원문", "회사", JobRole.BACKEND, "신입", "백엔드 개발자", "정규직",
                LocalDate.of(2026, 12, 31), requirements
        );
    }

    private AdminJobPostingSaveRequest.Requirement requirement(String name, String summary) {
        return new AdminJobPostingSaveRequest.Requirement(name, RequirementCategory.REQUIRED, summary);
    }
}

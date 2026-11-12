package com.shoutoutz.api.jobposting.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AdminJobPostingUpdateRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "　　", " ", " \n"})
    @DisplayName("원문이 없거나 유니코드 공백뿐이면 보내지 않은 것으로 보고 null 로 둔 채 통과한다.")
    void treatsMissingContentAsNull(String content) {
        AdminJobPostingUpdateRequest request = withContent(content);

        assertThat(request.content()).isNull();
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("원문에 글자가 있으면 공백을 자르지 않고 그대로 둔다.")
    void keepsContentAsIs() {
        AdminJobPostingUpdateRequest request = withContent("  원문\n");

        assertThat(request.content()).isEqualTo("  원문\n");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("공고 원문은 Unicode code point 기준으로 10,000자까지 허용하고, 넘으면 거절한다.")
    void rejectsContentOverMaxLength() {
        String max = "😀".repeat(AdminJobPostingSaveRequest.MAX_CONTENT_LENGTH);

        assertThat(validator.validate(withContent(max))).isEmpty();
        assertThat(violatedPaths(withContent(max + "😀"))).containsExactly("content");
    }

    @Test
    @DisplayName("문자열의 앞뒤 공백을 자르고, 비어 있는 선택값은 null 로 둔다.")
    void sanitizesStrings() {
        AdminJobPostingUpdateRequest request = new AdminJobPostingUpdateRequest(
                "  https://example.com/jobs/1  ", null, "  회사  ", JobRole.BACKEND,
                "   ", " 백엔드 개발자 ", "", null, null, List.of(requirement("Spring", "설명"))
        );

        assertThat(request.sourceUrl()).isEqualTo("https://example.com/jobs/1");
        assertThat(request.companyName()).isEqualTo("회사");
        assertThat(request.careerRequirement()).isNull();
        assertThat(request.title()).isEqualTo("백엔드 개발자");
        assertThat(request.employmentType()).isNull();
    }

    @Test
    @DisplayName("원문 말고 필수 값이 없으면 거절한다.")
    void rejectsMissingRequiredFields() {
        AdminJobPostingUpdateRequest request = new AdminJobPostingUpdateRequest(
                null, null, "　", null, null, null, null, null, null, List.of(requirement("Spring", "설명"))
        );

        assertThat(violatedPaths(request))
                .containsExactlyInAnyOrder("sourceUrl", "companyName", "jobRole", "title");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("원문이 없으면 역량을 비워 보내도 통과하고, 빈 목록으로 둔다.")
    void acceptsMissingRequirementsWithoutContent(List<AdminJobPostingSaveRequest.Requirement> requirements) {
        AdminJobPostingUpdateRequest request = request(null, requirements);

        assertThat(request.requirements()).isEmpty();
        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("원문을 보내면서 역량을 비우면 거절한다.")
    void rejectsMissingRequirementsWithContent(List<AdminJobPostingSaveRequest.Requirement> requirements) {
        assertThat(violatedPaths(request("원문", requirements))).containsExactly("requirementsPresentWithContent");
    }

    @Test
    @DisplayName("역량을 보내면 원문이 없어도 개수와 항목을 검증한다.")
    void validatesRequirementsWithoutContent() {
        List<AdminJobPostingSaveRequest.Requirement> fiftyOne =
                Collections.nCopies(51, requirement("Spring", "설명"));
        AdminJobPostingUpdateRequest invalid = request(null, List.of(
                new AdminJobPostingSaveRequest.Requirement(" ", null, "설명")
        ));

        assertThat(violatedPaths(request(null, fiftyOne))).containsExactly("requirements");
        assertThat(violatedPaths(invalid))
                .containsExactlyInAnyOrder("requirements[0].name", "requirements[0].category");
    }

    private List<String> violatedPaths(AdminJobPostingUpdateRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .toList();
    }

    private AdminJobPostingUpdateRequest withContent(String content) {
        return new AdminJobPostingUpdateRequest(
                "https://example.com/jobs/1", content, "회사", JobRole.BACKEND, "신입", "백엔드 개발자", "정규직",
                LocalDate.of(2026, 12, 31), null, List.of(requirement("Spring", "설명"))
        );
    }

    private AdminJobPostingUpdateRequest request(String content,
                                                 List<AdminJobPostingSaveRequest.Requirement> requirements) {
        return new AdminJobPostingUpdateRequest(
                "https://example.com/jobs/1", content, "회사", JobRole.BACKEND, null, "백엔드 개발자", null,
                null, null, requirements
        );
    }

    private AdminJobPostingSaveRequest.Requirement requirement(String name, String summary) {
        return new AdminJobPostingSaveRequest.Requirement(name, RequirementCategory.REQUIRED, summary);
    }
}

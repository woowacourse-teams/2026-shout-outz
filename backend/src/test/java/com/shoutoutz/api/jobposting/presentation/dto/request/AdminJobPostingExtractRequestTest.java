package com.shoutoutz.api.jobposting.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminJobPostingExtractRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("주소의 앞뒤 공백만 자르고 공고 원문은 그대로 둔다.")
    void sanitizesOnlyUrl() {
        AdminJobPostingExtractRequest request = new AdminJobPostingExtractRequest(" https://example.com/jobs/1 ", "  원문\n");

        assertThat(request.sourceUrl()).isEqualTo("https://example.com/jobs/1");
        assertThat(request.content()).isEqualTo("  원문\n");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("주소가 없거나 형식이 틀리고, 원문이 공백뿐이면 거절한다.")
    void rejectsInvalidValues() {
        assertThat(validator.validate(new AdminJobPostingExtractRequest(null, " \n ")))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("sourceUrl", "content");
        assertThat(validator.validate(new AdminJobPostingExtractRequest("example.com", "원문")))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("sourceUrl");
    }

    @Test
    @DisplayName("공고 원문이 10,000자를 초과하면 거절한다.")
    void rejectsContentOverMaxLength() {
        AdminJobPostingExtractRequest request = new AdminJobPostingExtractRequest(
                "https://example.com/jobs/1", "가".repeat(AdminJobPostingSaveRequest.MAX_CONTENT_LENGTH + 1));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("content");
    }

    @Test
    @DisplayName("원문이 전각 공백 같은 유니코드 공백뿐이면 LLM 을 부르기 전에 거절한다.")
    void rejectsUnicodeWhitespaceOnlyContent() {
        AdminJobPostingExtractRequest request = new AdminJobPostingExtractRequest(
                "https://example.com/jobs/1", "\u3000\u00A0\n");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("content");
    }
}

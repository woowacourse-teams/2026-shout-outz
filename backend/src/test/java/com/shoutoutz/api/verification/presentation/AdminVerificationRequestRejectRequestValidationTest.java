package com.shoutoutz.api.verification.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.verification.presentation.dto.request.AdminVerificationRequestRejectRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class AdminVerificationRequestRejectRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 반려_사유는_앞뒤_공백을_제거하고_Unicode_code_point_기준_100자까지_허용한다() {
        AdminVerificationRequestRejectRequest request =
                new AdminVerificationRequestRejectRequest("  " + "😀".repeat(100) + "  ");

        assertThat(request.reason()).isEqualTo("😀".repeat(100));
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 반려_사유가_101_code_point이면_거절한다() {
        AdminVerificationRequestRejectRequest request =
                new AdminVerificationRequestRejectRequest("😀".repeat(101));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("reason");
    }

    @Test
    void 반려_사유가_비어_있으면_거절한다() {
        AdminVerificationRequestRejectRequest request =
                new AdminVerificationRequestRejectRequest("  ");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("reason");
    }
}

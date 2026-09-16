package com.shoutoutz.api.verification.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import com.shoutoutz.api.verification.presentation.dto.request.AdminVerificationRequestFindAllRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class AdminVerificationRequestFindAllRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 상태와_커서의_앞뒤_공백을_제거하고_기본값을_사용한다() {
        AdminVerificationRequestFindAllRequest request =
                new AdminVerificationRequestFindAllRequest(" PENDING ", null, " cursor ");

        assertThat(request.status()).isEqualTo("PENDING");
        assertThat(request.cursor()).isEqualTo("cursor");
        assertThat(request.resolvedStatus()).isEqualTo(VerificationRequestStatus.PENDING);
        assertThat(request.resolvedSize()).isEqualTo(20);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 허용되지_않은_상태는_거절한다() {
        AdminVerificationRequestFindAllRequest request =
                new AdminVerificationRequestFindAllRequest("PROCESSING", null, null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("status");
    }

    @Test
    void size는_1부터_100까지만_허용한다() {
        AdminVerificationRequestFindAllRequest tooSmall =
                new AdminVerificationRequestFindAllRequest(null, 0, null);
        AdminVerificationRequestFindAllRequest tooLarge =
                new AdminVerificationRequestFindAllRequest(null, 101, null);

        assertThat(validator.validate(tooSmall))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("size");
        assertThat(validator.validate(tooLarge))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("size");
    }
}

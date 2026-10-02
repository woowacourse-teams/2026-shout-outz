package com.shoutoutz.api.verification.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.verification.presentation.dto.request.UserVerificationRequestCreateRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class UserVerificationRequestCreateRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 닉네임은_앞뒤_공백을_제거하고_Unicode_code_point_기준_50자까지_허용한다() {
        UserVerificationRequestCreateRequest request = new UserVerificationRequestCreateRequest(
                UserType.WOOWACOURSE_CREW,
                "  " + "😀".repeat(50) + "  ",
                8,
                " BACKEND "
        );

        assertThat(request.nickname()).isEqualTo("😀".repeat(50));
        assertThat(request.track()).isEqualTo("BACKEND");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 닉네임이_51_code_point이면_거절한다() {
        UserVerificationRequestCreateRequest request = new UserVerificationRequestCreateRequest(
                UserType.WOOWACOURSE_CREW,
                "😀".repeat(51),
                8,
                "BACKEND"
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("nickname");
    }

    @Test
    void cohort가_0이면_거절한다() {
        UserVerificationRequestCreateRequest request = new UserVerificationRequestCreateRequest(
                UserType.WOOWACOURSE_CREW,
                "샤를",
                0,
                "BACKEND"
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("cohort");
    }
}

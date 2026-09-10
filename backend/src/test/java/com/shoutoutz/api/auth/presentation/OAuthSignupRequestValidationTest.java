package com.shoutoutz.api.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.auth.presentation.dto.request.OAuthSignupRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuthSignupRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("가입 표시 이름 길이를 유니코드 코드 포인트 기준으로 검증한다")
    void validateDisplayNameLength() {
        OAuthSignupRequest request = new OAuthSignupRequest(
                "zzaekkii",
                "😀".repeat(51)
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("displayName");
    }

    @Test
    @DisplayName("가입 핸들의 형식을 검증한다")
    void validateHandleFormat() {
        OAuthSignupRequest request = new OAuthSignupRequest(
                "잘못된 핸들",
                "재키"
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("handle");
    }
}

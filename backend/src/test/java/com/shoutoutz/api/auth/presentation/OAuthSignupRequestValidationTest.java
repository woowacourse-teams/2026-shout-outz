package com.shoutoutz.api.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.auth.presentation.dto.request.OAuthSignupRequest;
import com.shoutoutz.api.user.domain.profile.UserType;
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
                "😀".repeat(51),
                UserType.GENERAL,
                null,
                null
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("displayName");
    }

    @Test
    @DisplayName("우테코 크루 가입에는 트랙과 기수가 필요하다")
    void requireCrewCourseInformation() {
        OAuthSignupRequest request = new OAuthSignupRequest(
                "zzaekkii",
                "재키",
                UserType.WOOWACOURSE_CREW,
                null,
                null
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("track");
    }
}

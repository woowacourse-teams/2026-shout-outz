package com.shoutoutz.api.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.user.presentation.dto.request.UserProfileUpdateRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserProfileUpdateRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("프로필 수정 요청의 길이와 값 범위를 검증한다")
    void validateProfileFieldConstraints() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                "재키",
                "😀".repeat(201),
                0L,
                "http://github.com/zzaekkii",
                null
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("bio", "avatarImageId", "githubProfileUrl");
    }

    @ParameterizedTest
    @DisplayName("호스트가 없는 블로그 URL을 거절한다")
    @ValueSource(strings = {"https:///profile", "http://?x"})
    void rejectBlogUrlWithoutHost(String blogUrl) {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                "재키",
                null,
                null,
                null,
                blogUrl
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("blogUrl");
    }
}

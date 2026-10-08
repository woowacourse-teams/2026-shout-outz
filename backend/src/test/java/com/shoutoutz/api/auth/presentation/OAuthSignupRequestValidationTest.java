package com.shoutoutz.api.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.auth.presentation.dto.request.OAuthSignupRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OAuthSignupRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("가입 표시 이름 길이를 유니코드 코드 포인트 기준으로 검증한다")
    void validateDisplayNameLength() {
        OAuthSignupRequest request = new OAuthSignupRequest(
                "@zzaekkii",
                "😀".repeat(51),
                null,
                null,
                null
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("displayName");
    }

    @ParameterizedTest
    @DisplayName("가입 핸들의 형식을 검증한다")
    @ValueSource(strings = {
            "zzaekkii",
            "@",
            "a",
            "@잘못된핸들",
            "@user handle",
            "@user@handle",
            "@abcdefghijklmnopqrstuvwxyz12345"
    })
    void validateHandleFormat(String handle) {
        OAuthSignupRequest request = new OAuthSignupRequest(
                handle,
                "재키",
                null,
                null,
                null
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("handle");
    }

    @Test
    @DisplayName("200 코드 포인트의 소개와 유효한 URL을 허용한다")
    void acceptsValidAdditionalProfileInformation() {
        OAuthSignupRequest request = new OAuthSignupRequest(
                "@zzaekkii",
                "재키",
                "😀".repeat(200),
                "https://github.com/zzaekkii/",
                "https://zzaekkii.dev/posts?tag=java#intro"
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("소개가 200 코드 포인트를 넘으면 거절한다")
    void rejectsTooLongBio() {
        OAuthSignupRequest request = new OAuthSignupRequest(
                "@zzaekkii", "재키", "😀".repeat(201), null, null
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("bio");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "http://github.com/zzaekkii", "https://github.com/zzaekkii/repo",
            "https://github.com/zzaekkii?tab=repositories", "https://example.com/zzaekkii"})
    @DisplayName("GitHub 프로필 URL 형식이 아니면 거절한다")
    void rejectsInvalidGithubProfileUrl(String githubProfileUrl) {
        OAuthSignupRequest request = new OAuthSignupRequest(
                "@zzaekkii", "재키", null, githubProfileUrl, null
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("githubProfileUrl");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "not-a-url", "https:///profile", "http://?x",
            "javascript:alert(1)", "https://example.com/a b"})
    @DisplayName("HTTP 또는 HTTPS 주소 형식이 아닌 블로그 URL은 거절한다")
    void rejectsInvalidBlogUrl(String blogUrl) {
        OAuthSignupRequest request = new OAuthSignupRequest(
                "@zzaekkii", "재키", null, null, blogUrl
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("blogUrl");
    }
}

package com.shoutoutz.api.user.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.CodePointLength;

public record UserProfileUpdateRequest(
        @NotBlank(message = "displayName은 필수입니다.")
        @CodePointLength(max = 50, message = "displayName은 50자를 초과할 수 없습니다.")
        String displayName,

        @CodePointLength(max = 200, message = "bio는 200자를 초과할 수 없습니다.")
        String bio,

        @Positive(message = "avatarImageId는 0보다 커야 합니다.")
        Long avatarImageId,

        @Pattern(
                regexp = "^https://github\\.com/[^/\\s?#]+/?$",
                message = "githubProfileUrl 형식이 올바르지 않습니다."
        )
        String githubProfileUrl,

        @Pattern(
                regexp = "^https?://[^\\s/?#:]+(?::\\d{1,5})?(?:[/?#][^\\s]*)?$",
                message = "blogUrl 형식이 올바르지 않습니다."
        )
        String blogUrl
) {
}

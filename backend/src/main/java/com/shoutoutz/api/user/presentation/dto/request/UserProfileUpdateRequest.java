package com.shoutoutz.api.user.presentation.dto.request;

import com.shoutoutz.api.common.validator.CodePointSize;
import com.shoutoutz.api.user.application.dto.command.UserProfileUpdateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record UserProfileUpdateRequest(
        @NotBlank(message = "displayName은 필수입니다.")
        @CodePointSize(max = 50, message = "displayName은 50자를 초과할 수 없습니다.")
        String displayName,

        @CodePointSize(max = 200, message = "bio는 200자를 초과할 수 없습니다.")
        String bio,

        @Positive(message = "avatarImageId는 0보다 커야 합니다.")
        Long avatarImageId,

        @Pattern(
                regexp = "^https://github\\.com/[^/\\s?#]+/?$",
                message = "githubProfileUrl 형식이 올바르지 않습니다."
        )
        String githubProfileUrl,

        @Pattern(regexp = "^https?://[^\\s]+$", message = "blogUrl 형식이 올바르지 않습니다.")
        String blogUrl
) {

    public UserProfileUpdateCommand toCommand(long userId) {
        return new UserProfileUpdateCommand(
                userId,
                displayName,
                bio,
                avatarImageId,
                githubProfileUrl,
                blogUrl
        );
    }
}

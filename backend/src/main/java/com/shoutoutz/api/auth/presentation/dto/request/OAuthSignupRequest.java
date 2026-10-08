package com.shoutoutz.api.auth.presentation.dto.request;

import static com.shoutoutz.api.user.domain.account.Handle.HANDLE_FORMAT_REGEX;

import com.shoutoutz.api.auth.application.command.OAuthSignupCommand;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.CodePointLength;

public record OAuthSignupRequest(
        @NotBlank(message = "handle은 필수입니다.")
        @Pattern(
                regexp = HANDLE_FORMAT_REGEX,
                message = "handle 형식이 올바르지 않습니다."
        )
        String handle,
        @NotBlank(message = "displayName은 필수입니다.")
        @CodePointLength(max = 50, message = "displayName은 50자를 초과할 수 없습니다.")
        String displayName,
        @CodePointLength(max = 200, message = "bio는 200자를 초과할 수 없습니다.")
        String bio,
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

    public OAuthSignupCommand toCommand(OAuthIdentity identity) {
        return new OAuthSignupCommand(
                handle,
                displayName,
                bio,
                githubProfileUrl,
                blogUrl,
                identity
        );
    }
}

package com.shoutoutz.api.auth.presentation.dto.request;

import static com.shoutoutz.api.user.domain.account.Handle.HANDLE_FORMAT_REGEX;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OAuthSignupHandleAvailabilityRequest(
        @NotBlank(message = "handle은 필수입니다.")
        @Pattern(
                regexp = HANDLE_FORMAT_REGEX,
                message = "handle 형식이 올바르지 않습니다."
        )
        String handle
) {
}

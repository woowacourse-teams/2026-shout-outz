package com.shoutoutz.api.auth.presentation.dto.request;

import com.shoutoutz.api.auth.application.command.OAuthSignupCommand;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.common.validator.CodePointSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OAuthSignupRequest(
        @NotBlank(message = "handle은 필수입니다.")
        @Pattern(
                regexp = "^[A-Za-z0-9_-]{2,30}$",
                message = "handle 형식이 올바르지 않습니다."
        )
        String handle,
        @NotBlank(message = "displayName은 필수입니다.")
        @CodePointSize(max = 50, message = "displayName은 50자를 초과할 수 없습니다.")
        String displayName
) {

    public OAuthSignupCommand toCommand(OAuthIdentity identity) {
        return new OAuthSignupCommand(
                handle,
                displayName,
                identity
        );
    }
}

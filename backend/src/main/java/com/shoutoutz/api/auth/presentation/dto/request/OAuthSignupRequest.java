package com.shoutoutz.api.auth.presentation.dto.request;

import com.shoutoutz.api.auth.application.dto.command.OAuthSignupCommand;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.user.domain.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OAuthSignupRequest(
        @NotBlank(message = "handle은 필수입니다.") String handle,
        @NotBlank(message = "displayName은 필수입니다.") String displayName,
        @NotNull(message = "userType은 필수입니다.") UserType userType,
        String track,
        Short cohort
) {

    public OAuthSignupCommand toCommand(OAuthIdentity identity) {
        return new OAuthSignupCommand(
                handle,
                displayName,
                userType,
                track,
                cohort,
                identity
        );
    }
}

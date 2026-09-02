package com.shoutoutz.api.auth.presentation.dto.request;

import com.shoutoutz.api.auth.application.dto.command.OAuthSignupCommand;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.user.domain.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OAuthSignupRequest(
        @NotBlank String handle,
        @NotBlank String displayName,
        @NotNull UserType userType,
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

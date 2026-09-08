package com.shoutoutz.api.auth.presentation.dto.request;

import com.shoutoutz.api.auth.application.dto.command.OAuthSignupCommand;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.presentation.validation.ValidOAuthSignupProfile;
import com.shoutoutz.api.common.validator.CodePointSize;
import com.shoutoutz.api.user.domain.profile.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@ValidOAuthSignupProfile
public record OAuthSignupRequest(
        @NotBlank(message = "handle은 필수입니다.") String handle,
        @NotBlank(message = "displayName은 필수입니다.")
        @CodePointSize(max = 50, message = "displayName은 50자를 초과할 수 없습니다.")
        String displayName,
        @NotNull(message = "userType은 필수입니다.") UserType userType,
        @CodePointSize(max = 10, message = "track은 10자를 초과할 수 없습니다.") String track,
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

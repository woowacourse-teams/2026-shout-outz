package com.shoutoutz.api.auth.presentation.dto.response;

import com.shoutoutz.api.auth.application.command.OAuthSignupResult;

public record OAuthSignupResponse(
        Long userId
) {

    public static OAuthSignupResponse from(OAuthSignupResult result) {
        return new OAuthSignupResponse(result.userId());
    }
}

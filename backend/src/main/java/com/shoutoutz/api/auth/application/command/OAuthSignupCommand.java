package com.shoutoutz.api.auth.application.command;

import com.shoutoutz.api.auth.domain.OAuthIdentity;

public record OAuthSignupCommand(
        String handle,
        String displayName,
        String bio,
        String githubProfileUrl,
        String blogUrl,
        OAuthIdentity identity
) {

    public OAuthSignupCommand {
        if (identity == null) {
            throw new IllegalArgumentException("OAuth 신원은 필수입니다.");
        }
    }
}

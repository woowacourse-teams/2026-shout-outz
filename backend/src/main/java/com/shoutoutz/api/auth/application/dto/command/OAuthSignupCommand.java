package com.shoutoutz.api.auth.application.dto.command;

import com.shoutoutz.api.auth.domain.OAuthIdentity;

public record OAuthSignupCommand(
        String handle,
        String displayName,
        OAuthIdentity identity
) {

    public OAuthSignupCommand {
        if (identity == null) {
            throw new IllegalArgumentException("OAuth 신원은 필수입니다.");
        }
    }
}

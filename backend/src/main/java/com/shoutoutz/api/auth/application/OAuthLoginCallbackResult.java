package com.shoutoutz.api.auth.application;

import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.user.domain.UserRole;

public record OAuthLoginCallbackResult(
        Status status,
        Long userId,
        UserRole role,
        OAuthIdentity identity
) {

    public static OAuthLoginCallbackResult authenticated(Long userId, UserRole role) {
        return new OAuthLoginCallbackResult(Status.AUTHENTICATED, userId, role, null);
    }

    public static OAuthLoginCallbackResult signupRequired(OAuthIdentity identity) {
        return new OAuthLoginCallbackResult(Status.SIGNUP_REQUIRED, null, null, identity);
    }

    public enum Status {
        AUTHENTICATED,
        SIGNUP_REQUIRED
    }
}

package com.shoutoutz.api.auth.application.command;

import com.shoutoutz.api.user.domain.account.UserRole;

public record OAuthSignupResult(
        Long userId,
        UserRole role
) {
}

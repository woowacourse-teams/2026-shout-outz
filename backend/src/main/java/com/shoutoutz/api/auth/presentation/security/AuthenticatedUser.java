package com.shoutoutz.api.auth.presentation.security;

import com.shoutoutz.api.user.domain.account.UserRole;

public record AuthenticatedUser(
        Long userId,
        UserRole role
) {
}

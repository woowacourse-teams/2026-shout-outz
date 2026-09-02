package com.shoutoutz.api.auth.presentation;

import com.shoutoutz.api.user.domain.UserRole;

public record AuthenticatedSession(
        Long userId,
        UserRole role
) {
}

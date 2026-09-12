package com.shoutoutz.api.auth.presentation.session;

import com.shoutoutz.api.user.domain.account.UserRole;

public record AuthenticatedSession(
        Long userId,
        UserRole role
) {
}

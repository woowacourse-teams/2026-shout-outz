package com.shoutoutz.api.auth.application.dto.result;

import com.shoutoutz.api.user.domain.UserRole;

public record OAuthSignupResult(
        Long userId,
        UserRole role
) {
}

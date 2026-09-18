package com.shoutoutz.api.adminbootstrap.application;

import com.shoutoutz.api.user.domain.account.UserRole;

public record AdminBootstrapResult(
        Long userId,
        UserRole role
) {
}

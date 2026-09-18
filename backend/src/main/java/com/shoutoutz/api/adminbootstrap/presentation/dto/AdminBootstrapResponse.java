package com.shoutoutz.api.adminbootstrap.presentation.dto;

import com.shoutoutz.api.adminbootstrap.application.AdminBootstrapResult;
import com.shoutoutz.api.user.domain.account.UserRole;

public record AdminBootstrapResponse(
        Long userId,
        UserRole role
) {

    public static AdminBootstrapResponse from(AdminBootstrapResult result) {
        return new AdminBootstrapResponse(result.userId(), result.role());
    }
}

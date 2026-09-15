package com.shoutoutz.api.auth.presentation.security;

import com.shoutoutz.api.user.domain.account.UserRole;

public record AuthenticatedUser(
        Long userId,
        UserRole role
) {

    /**
     * 선택적 로그인(@LoginUser(required = false))에서 비로그인이면 null을 반환한다.
     */
    public static Long userIdOrNull(AuthenticatedUser user) {
        return user == null ? null : user.userId();
    }
}

package com.shoutoutz.api.auth.presentation.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.user.domain.account.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthenticatedUserTest {

    @Test
    @DisplayName("로그인 사용자가 있으면 사용자 ID를 반환한다.")
    void returnsUserIdWhenAuthenticated() {
        assertThat(AuthenticatedUser.userIdOrNull(new AuthenticatedUser(7L, UserRole.USER))).isEqualTo(7L);
    }

    @Test
    @DisplayName("비로그인이면 null을 반환한다.")
    void returnsNullWhenAnonymous() {
        assertThat(AuthenticatedUser.userIdOrNull(null)).isNull();
    }
}

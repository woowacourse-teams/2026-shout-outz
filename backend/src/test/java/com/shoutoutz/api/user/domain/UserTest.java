package com.shoutoutz.api.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    @DisplayName("사용자를 초기화하면 기본 상태와 권한을 가진다")
    void initializesUserWithDefaultStatusAndRole() {
        User user = User.initialize(new Handle("zzaekkii"));

        assertThat(user.getId()).isNull();
        assertThat(user.getHandle()).isEqualTo(new Handle("zzaekkii"));
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getLastLoginAt()).isNull();
        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.getPurgedAt()).isNull();
    }

    @Test
    @DisplayName("탈퇴 상태와 탈퇴 시각은 함께 존재해야 한다")
    void requiresDeletedStatusAndDeletedAtTogether() {
        assertThatThrownBy(() -> User.builder()
                .id(1L)
                .handle(new Handle("zzaekkii"))
                .status(UserStatus.DELETED)
                .role(UserRole.USER)
                .build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> User.builder()
                .id(1L)
                .handle(new Handle("zzaekkii"))
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .deletedAt(Instant.now())
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }
}

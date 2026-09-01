package com.shoutoutz.api.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class UserTest {

    @Test
    @DisplayName("사용자를 초기화하면 기본 상태와 권한을 가진다")
    void initializesUserWithDefaultStatusAndRole() {
        User user = User.initialize("zzaekkii");

        assertThat(user.getId()).isNull();
        assertThat(user.getHandle()).isEqualTo("zzaekkii");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getLastLoginAt()).isNull();
        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.getPurgedAt()).isNull();
    }

    @ParameterizedTest
    @DisplayName("유효하지 않은 핸들로 사용자를 생성할 수 없다")
    @NullSource
    @ValueSource(strings = {
            "",
            "a",
            "user handle",
            "user@handle",
            "abcdefghijklmnopqrstuvwxyz12345"
    })
    void rejectsInvalidHandle(String handle) {
        assertThatThrownBy(() -> User.initialize(handle))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("탈퇴 상태와 탈퇴 시각은 함께 존재해야 한다")
    void requiresDeletedStatusAndDeletedAtTogether() {
        assertThatThrownBy(() -> User.builder()
                .id(1L)
                .handle("zzaekkii")
                .status(UserStatus.DELETED)
                .role(UserRole.USER)
                .build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> User.builder()
                .id(1L)
                .handle("zzaekkii")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .deletedAt(Instant.now())
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.shoutoutz.api.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class UserTest {

    @Test
    void 사용자를_초기화하면_기본_상태와_권한을_가진다() {
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
    @NullSource
    @ValueSource(strings = {
            "",
            "a",
            "user handle",
            "user@handle",
            "abcdefghijklmnopqrstuvwxyz12345"
    })
    void 유효하지_않은_핸들로_사용자를_생성할_수_없다(String handle) {
        assertThatThrownBy(() -> User.initialize(handle))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 탈퇴_상태와_탈퇴_시각은_함께_존재해야_한다() {
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

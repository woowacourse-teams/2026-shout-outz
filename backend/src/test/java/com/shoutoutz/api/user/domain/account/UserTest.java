package com.shoutoutz.api.user.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    @DisplayName("사용자를 초기화하면 기본 상태와 권한을 가진다")
    void initializesUserWithDefaultStatusAndRole() {
        User user = User.initialize("zzaekkii");

        assertThat(user.getId()).isNull();
        assertThat(user.getHandle()).isEqualTo(new Handle("zzaekkii"));
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getLastLoginAt()).isNull();
        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.getPurgedAt()).isNull();
    }

    @Test
    @DisplayName("공백 핸들로 사용자를 생성할 수 없다")
    void rejectsBlankHandleWhenInitializingUser() {
        assertThatThrownBy(() -> User.initialize(" "))
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

    @Test
    @DisplayName("활성 사용자가 로그인하면 마지막 로그인 시각을 기록한다")
    void recordsActiveUserLogin() {
        Instant loginAt = Instant.parse("2026-09-03T00:00:00Z");
        User user = User.builder()
                .id(1L)
                .handle("sangjun")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();

        User loggedInUser = user.recordLogin(loginAt);

        assertThat(loggedInUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(loggedInUser.getLastLoginAt()).isEqualTo(loginAt);
    }

    @Test
    @DisplayName("탈퇴 유예 사용자가 로그인하면 계정을 복구한다")
    void restoresDeletedUserWhenLoggingIn() {
        Instant loginAt = Instant.parse("2026-09-03T00:00:00Z");
        User deletedUser = User.builder()
                .id(1L)
                .handle("sangjun")
                .status(UserStatus.DELETED)
                .role(UserRole.USER)
                .deletedAt(Instant.parse("2026-09-02T00:00:00Z"))
                .build();

        User restoredUser = deletedUser.recordLogin(loginAt);

        assertThat(restoredUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(restoredUser.getDeletedAt()).isNull();
        assertThat(restoredUser.getLastLoginAt()).isEqualTo(loginAt);
    }

    @Test
    @DisplayName("정지된 사용자는 로그인할 수 없다")
    void rejectsBannedUserLogin() {
        User bannedUser = User.builder()
                .id(1L)
                .handle("sangjun")
                .status(UserStatus.BANNED)
                .role(UserRole.USER)
                .build();

        assertThatThrownBy(() -> bannedUser.recordLogin(Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("개인정보가 파기된 사용자는 로그인으로 복구할 수 없다")
    void rejectsPurgedUserLogin() {
        User purgedUser = User.builder()
                .id(1L)
                .handle("sangjun")
                .status(UserStatus.DELETED)
                .role(UserRole.USER)
                .deletedAt(Instant.parse("2026-08-01T00:00:00Z"))
                .purgedAt(Instant.parse("2026-09-01T00:00:00Z"))
                .build();

        assertThatThrownBy(() -> purgedUser.recordLogin(Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }
}

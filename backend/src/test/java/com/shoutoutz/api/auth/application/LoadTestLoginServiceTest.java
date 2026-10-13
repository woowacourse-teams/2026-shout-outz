package com.shoutoutz.api.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.shoutoutz.api.auth.exception.AuthErrorCode;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserErrorCode;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.account.UserStatus;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;

class LoadTestLoginServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private LoadTestLoginService loadTestLoginService;

    @BeforeEach
    void setUp() {
        loadTestLoginService = new LoadTestLoginService(userRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"@lt-00001", "lt-00001", "  lt-00001  "})
    @DisplayName("handle에 @가 없거나 앞뒤 공백이 있어도 같은 사용자를 찾는다.")
    void findsActiveUserRegardlessOfHandlePrefix(String handle) {
        User user = user(UserStatus.ACTIVE, UserRole.USER, null);
        given(userRepository.findByHandle("@lt-00001")).willReturn(Optional.of(user));

        User result = loadTestLoginService.findLoginUser(handle);

        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("사용자 데이터를 저장하지 않는다.")
    void doesNotSaveUser() {
        given(userRepository.findByHandle("@lt-00001"))
                .willReturn(Optional.of(user(UserStatus.ACTIVE, UserRole.USER, null)));

        loadTestLoginService.findLoginUser("lt-00001");

        verify(userRepository, never()).save(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("존재하지 않는 handle로는 로그인할 수 없다.")
    void throwsWhenUserNotFound() {
        given(userRepository.findByHandle("@lt-00001")).willReturn(Optional.empty());

        assertThatThrownBy(() -> loadTestLoginService.findLoginUser("lt-00001"))
                .isInstanceOf(EntityNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("정지된 사용자는 로그인할 수 없다.")
    void rejectsBannedUser() {
        given(userRepository.findByHandle("@lt-00001"))
                .willReturn(Optional.of(user(UserStatus.BANNED, UserRole.USER, null)));

        assertNotAllowed("lt-00001");
    }

    @Test
    @DisplayName("탈퇴한 사용자는 로그인할 수 없다.")
    void rejectsDeletedUser() {
        given(userRepository.findByHandle("@lt-00001")).willReturn(Optional.of(
                user(UserStatus.DELETED, UserRole.USER, Instant.parse("2026-10-01T00:00:00Z"))
        ));

        assertNotAllowed("lt-00001");
    }

    @Test
    @DisplayName("관리자는 로그인할 수 없다.")
    void rejectsAdmin() {
        given(userRepository.findByHandle("@lt-00001"))
                .willReturn(Optional.of(user(UserStatus.ACTIVE, UserRole.ADMIN, null)));

        assertNotAllowed("lt-00001");
    }

    private void assertNotAllowed(String handle) {
        assertThatThrownBy(() -> loadTestLoginService.findLoginUser(handle))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.LOAD_TEST_LOGIN_NOT_ALLOWED);
    }

    private User user(UserStatus status, UserRole role, Instant deletedAt) {
        return User.builder()
                .id(1L)
                .handle("@lt-00001")
                .status(status)
                .role(role)
                .deletedAt(deletedAt)
                .build();
    }
}

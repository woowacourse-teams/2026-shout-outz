package com.shoutoutz.api.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.user.domain.Handle;
import com.shoutoutz.api.user.domain.User;
import com.shoutoutz.api.user.domain.UserRepository;
import com.shoutoutz.api.user.domain.UserRole;
import com.shoutoutz.api.user.domain.UserStatus;
import com.shoutoutz.api.user.infrastructure.jpa.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    @DisplayName("사용자를 저장하고 ID로 조회한다")
    void savesAndFindsUserById() {
        User savedUser = userRepository.save(User.initialize("zzaekkii"));

        User foundUser = userRepository.findById(savedUser.getId()).orElseThrow();
        UserEntity savedEntity = userJpaRepository.findById(savedUser.getId()).orElseThrow();

        assertThat(savedUser.getId()).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(savedUser.getId());
        assertThat(foundUser.getHandle()).isEqualTo(new Handle("zzaekkii"));
        assertThat(foundUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(foundUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(savedEntity.getCreatedAt()).isNotNull();
        assertThat(savedEntity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID를 조회하면 빈 결과를 반환한다")
    void returnsEmptyWhenUserDoesNotExist() {
        assertThat(userRepository.findById(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    @DisplayName("핸들은 대소문자를 구분하지 않고 유일해야 한다")
    void rejectsDuplicateHandleIgnoringCase() {
        userRepository.save(User.initialize("dahye"));

        assertThatThrownBy(() -> {
            userRepository.save(User.initialize("DaHye"));
            userJpaRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}

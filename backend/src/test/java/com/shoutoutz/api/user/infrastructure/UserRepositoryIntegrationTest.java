package com.shoutoutz.api.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.user.domain.User;
import com.shoutoutz.api.user.domain.UserRepository;
import com.shoutoutz.api.user.domain.UserRole;
import com.shoutoutz.api.user.domain.UserStatus;
import com.shoutoutz.api.user.infrastructure.jpa.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    void 사용자를_저장하고_ID로_조회한다() {
        User savedUser = userRepository.save(User.initialize("zzaekkii"));

        User foundUser = userRepository.findById(savedUser.getId()).orElseThrow();
        UserEntity savedEntity = userJpaRepository.findById(savedUser.getId()).orElseThrow();

        assertThat(savedUser.getId()).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(savedUser.getId());
        assertThat(foundUser.getHandle()).isEqualTo("zzaekkii");
        assertThat(foundUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(foundUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(savedEntity.getCreatedAt()).isNotNull();
        assertThat(savedEntity.getUpdatedAt()).isNotNull();
    }

    @Test
    void 존재하지_않는_사용자_ID를_조회하면_빈_결과를_반환한다() {
        assertThat(userRepository.findById(Long.MAX_VALUE)).isEmpty();
    }
}

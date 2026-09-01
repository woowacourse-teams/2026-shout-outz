package com.shoutoutz.api.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.user.domain.ProfileDisplayName;
import com.shoutoutz.api.user.domain.User;
import com.shoutoutz.api.user.domain.UserProfile;
import com.shoutoutz.api.user.domain.UserProfileRepository;
import com.shoutoutz.api.user.domain.UserRepository;
import com.shoutoutz.api.user.domain.UserType;
import com.shoutoutz.api.user.infrastructure.jpa.UserProfileJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserProfileRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserProfileJpaRepository userProfileJpaRepository;

    @Test
    @DisplayName("사용자 프로필을 저장하고 사용자 ID로 조회한다")
    void savesAndFindsUserProfileByUserId() {
        User savedUser = userRepository.save(User.initialize("zzaekkii-profile"));
        UserProfile profile = UserProfile.initialize(savedUser.getId(), "재끼");

        UserProfile savedProfile = userProfileRepository.save(profile);
        UserProfile foundProfile = userProfileRepository.findByUserId(savedUser.getId()).orElseThrow();
        UserProfileEntity savedEntity = userProfileJpaRepository.findById(savedUser.getId()).orElseThrow();

        assertThat(savedProfile.getUserId()).isEqualTo(savedUser.getId());
        assertThat(foundProfile.getDisplayName()).isEqualTo(new ProfileDisplayName("재끼"));
        assertThat(foundProfile.getUserType()).isEqualTo(UserType.GENERAL);
        assertThat(savedEntity.getCreatedAt()).isNotNull();
        assertThat(savedEntity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 프로필을 조회하면 빈 결과를 반환한다")
    void returnsEmptyWhenUserProfileDoesNotExist() {
        assertThat(userProfileRepository.findByUserId(Long.MAX_VALUE)).isEmpty();
    }
}

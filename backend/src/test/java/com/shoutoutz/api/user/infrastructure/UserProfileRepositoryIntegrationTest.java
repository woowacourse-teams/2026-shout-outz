package com.shoutoutz.api.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.profile.ProfileDisplayName;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.user.infrastructure.jpa.UserProfileJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UserProfileRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserProfileJpaRepository userProfileJpaRepository;

    @Autowired
    private MediaMetadataRepository mediaMetadataRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("사용자 프로필을 저장하고 사용자 ID로 조회한다")
    void savesAndFindsUserProfileByUserId() {
        User savedUser = userRepository.save(User.initialize("zzaekkii-profile"));
        UserProfile profile = UserProfile.initialize(savedUser.getId(), "재키");

        UserProfile savedProfile = userProfileRepository.save(profile);
        UserProfile foundProfile = userProfileRepository.findByUserId(savedUser.getId()).orElseThrow();
        UserProfileEntity savedEntity = userProfileJpaRepository.findById(savedUser.getId()).orElseThrow();

        assertThat(savedProfile.getUserId()).isEqualTo(savedUser.getId());
        assertThat(foundProfile.getDisplayName()).isEqualTo(new ProfileDisplayName("재키"));
        assertThat(foundProfile.getUserType()).isEqualTo(UserType.GENERAL);
        assertThat(savedEntity.getCreatedAt()).isNotNull();
        assertThat(savedEntity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("기존 사용자 프로필의 수정 정보를 저장한다")
    void updatesUserProfile() {
        User savedUser = userRepository.save(User.initialize("zzaekkii-profile-update"));
        UserProfile profile = userProfileRepository.save(
                UserProfile.initialize(savedUser.getId(), "재키")
        );

        userProfileRepository.save(profile.update(
                "새 이름",
                "새 소개",
                null,
                "https://github.com/zzaekkii",
                "https://zzaekkii.dev"
        ));

        UserProfile foundProfile = userProfileRepository.findByUserId(savedUser.getId()).orElseThrow();
        assertThat(foundProfile.getDisplayName()).isEqualTo(new ProfileDisplayName("새 이름"));
        assertThat(foundProfile.getBio()).isEqualTo("새 소개");
        assertThat(foundProfile.getGithubProfileUrl()).isEqualTo("https://github.com/zzaekkii");
        assertThat(foundProfile.getBlogUrl()).isEqualTo("https://zzaekkii.dev");
    }

    @Test
    @DisplayName("프로필의 선택 정보를 null로 수정하면 기존 값을 제거한다")
    void clearsOptionalUserProfileFields() {
        User savedUser = userRepository.save(User.initialize("zzaekkii-profile-clear"));
        Instant now = Instant.now();
        MediaMetadata avatar = mediaMetadataRepository.save(MediaMetadata.initialize(
                MediaPurpose.USER_AVATAR,
                savedUser.getId(),
                "media/user-profile-clear/avatar.png",
                "avatar.png",
                "image/png",
                100L,
                now.plusSeconds(300),
                now
        ));
        UserProfile profile = userProfileRepository.save(UserProfile.builder()
                .userId(savedUser.getId())
                .displayName("재키")
                .userType(UserType.GENERAL)
                .bio("기존 소개")
                .avatarImageId(avatar.getId())
                .githubProfileUrl("https://github.com/zzaekkii")
                .blogUrl("https://zzaekkii.dev")
                .build());
        entityManager.flush();
        entityManager.clear();

        UserProfile persistedProfile = userProfileRepository.findByUserId(profile.getUserId()).orElseThrow();
        userProfileRepository.save(persistedProfile.update("재키", null, null, null, null));
        entityManager.flush();
        entityManager.clear();

        UserProfile clearedProfile = userProfileRepository.findByUserId(savedUser.getId()).orElseThrow();
        assertThat(clearedProfile.getBio()).isNull();
        assertThat(clearedProfile.getAvatarImageId()).isNull();
        assertThat(clearedProfile.getGithubProfileUrl()).isNull();
        assertThat(clearedProfile.getBlogUrl()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 프로필을 조회하면 빈 결과를 반환한다")
    void returnsEmptyWhenUserProfileDoesNotExist() {
        assertThat(userProfileRepository.findByUserId(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    @DisplayName("데이터베이스는 우아한테크코스 코치의 기수를 허용하지 않는다")
    void rejectsCohortForWoowacourseCoachAtDatabase() {
        User savedUser = userRepository.save(User.initialize("sangjun-coach"));
        UserProfileEntity profileEntity = UserProfileEntity.builder()
                .userId(savedUser.getId())
                .displayName("상준")
                .userType(UserType.WOOWACOURSE_COACH)
                .track("BACKEND")
                .cohort((short) 8)
                .build();

        assertThatThrownBy(() -> userProfileJpaRepository.saveAndFlush(profileEntity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("데이터베이스는 200자를 초과하는 한 줄 소개를 허용하지 않는다")
    void rejectsBioLongerThanTwoHundredCharactersAtDatabase() {
        User savedUser = userRepository.save(User.initialize("long-bio-user"));
        UserProfileEntity profileEntity = UserProfileEntity.builder()
                .userId(savedUser.getId())
                .displayName("재키")
                .userType(UserType.GENERAL)
                .bio("가".repeat(201))
                .build();

        assertThatThrownBy(() -> userProfileJpaRepository.saveAndFlush(profileEntity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

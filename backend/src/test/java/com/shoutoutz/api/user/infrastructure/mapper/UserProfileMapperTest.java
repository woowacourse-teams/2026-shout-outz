package com.shoutoutz.api.user.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.user.domain.ProfileDisplayName;
import com.shoutoutz.api.user.domain.UserProfile;
import com.shoutoutz.api.user.domain.UserType;
import com.shoutoutz.api.user.infrastructure.UserProfileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserProfileMapperTest {

    @Test
    @DisplayName("사용자 프로필 도메인을 엔티티로 변환한다")
    void mapsDomainToEntity() {
        UserProfile profile = createUserProfile();

        UserProfileEntity entity = UserProfileMapper.toEntity(profile);

        assertThat(entity.getUserId()).isEqualTo(1L);
        assertThat(entity.getDisplayName()).isEqualTo("재키");
        assertThat(entity.getUserType()).isEqualTo(UserType.WOOWACOURSE_CREW);
        assertThat(entity.getTrack()).isEqualTo("BACKEND");
        assertThat(entity.getCohort()).isEqualTo((short) 8);
        assertThat(entity.getBio()).isEqualTo("소개");
        assertThat(entity.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(entity.getGithubProfileUrl()).isEqualTo("https://github.com/zzaekkii");
        assertThat(entity.getBlogUrl()).isEqualTo("https://example.com");
    }

    @Test
    @DisplayName("사용자 프로필 엔티티를 도메인으로 변환한다")
    void mapsEntityToDomain() {
        UserProfileEntity entity = UserProfileEntity.builder()
                .userId(1L)
                .displayName("재키")
                .userType(UserType.WOOWACOURSE_CREW)
                .track("BACKEND")
                .cohort((short) 8)
                .bio("소개")
                .avatarUrl("https://example.com/avatar.png")
                .githubProfileUrl("https://github.com/zzaekkii")
                .blogUrl("https://example.com")
                .build();

        UserProfile profile = UserProfileMapper.toDomain(entity);

        assertThat(profile.getUserId()).isEqualTo(1L);
        assertThat(profile.getDisplayName()).isEqualTo(new ProfileDisplayName("재키"));
        assertThat(profile.getUserType()).isEqualTo(UserType.WOOWACOURSE_CREW);
        assertThat(profile.getTrack()).isEqualTo("BACKEND");
        assertThat(profile.getCohort()).isEqualTo((short) 8);
        assertThat(profile.getBio()).isEqualTo("소개");
        assertThat(profile.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(profile.getGithubProfileUrl()).isEqualTo("https://github.com/zzaekkii");
        assertThat(profile.getBlogUrl()).isEqualTo("https://example.com");
    }

    private UserProfile createUserProfile() {
        return UserProfile.builder()
                .userId(1L)
                .displayName("재키")
                .userType(UserType.WOOWACOURSE_CREW)
                .track("BACKEND")
                .cohort((short) 8)
                .bio("소개")
                .avatarUrl("https://example.com/avatar.png")
                .githubProfileUrl("https://github.com/zzaekkii")
                .blogUrl("https://example.com")
                .build();
    }
}

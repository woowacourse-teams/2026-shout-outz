package com.shoutoutz.api.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserProfileTest {

    @Test
    @DisplayName("일반 사용자 프로필을 기본값으로 초기화한다")
    void initializesGeneralUserProfile() {
        UserProfile profile = UserProfile.initialize(1L, "재키");

        assertThat(profile.getUserId()).isEqualTo(1L);
        assertThat(profile.getDisplayName()).isEqualTo(new ProfileDisplayName("재키"));
        assertThat(profile.getUserType()).isEqualTo(UserType.GENERAL);
        assertThat(profile.getTrack()).isNull();
        assertThat(profile.getCohort()).isNull();
        assertThat(profile.getBio()).isNull();
        assertThat(profile.getAvatarUrl()).isNull();
        assertThat(profile.getGithubProfileUrl()).isNull();
        assertThat(profile.getBlogUrl()).isNull();
    }

    @Test
    @DisplayName("프로필 생성 시 문자열 표시 이름을 값 객체로 변환한다")
    void convertsRawDisplayNameWhenInitializingUserProfile() {
        assertThatThrownBy(() -> UserProfile.initialize(1L, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("사용자 ID와 표시 이름, 사용자 유형은 필수다")
    void requiresUserIdDisplayNameAndUserType() {
        assertThatThrownBy(() -> UserProfile.builder()
                .displayName("재키")
                .userType(UserType.GENERAL)
                .build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> UserProfile.builder()
                .userId(1L)
                .userType(UserType.GENERAL)
                .build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> UserProfile.builder()
                .userId(1L)
                .displayName("재키")
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("일반 사용자는 트랙과 기수를 가질 수 없다")
    void rejectsTrackAndCohortForGeneralUser() {
        assertThatThrownBy(() -> UserProfile.builder()
                .userId(1L)
                .displayName("재키")
                .userType(UserType.GENERAL)
                .track("BACKEND")
                .cohort((short) 8)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("우아한테크코스 크루는 트랙과 기수가 모두 필요하다")
    void requiresTrackAndCohortForWoowacourseCrew() {
        assertThatThrownBy(() -> UserProfile.builder()
                .userId(1L)
                .displayName("재키")
                .userType(UserType.WOOWACOURSE_CREW)
                .track("BACKEND")
                .build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> UserProfile.builder()
                .userId(1L)
                .displayName("재키")
                .userType(UserType.WOOWACOURSE_CREW)
                .cohort((short) 8)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("트랙과 기수가 있는 우아한테크코스 크루 프로필을 생성한다")
    void createsWoowacourseCrewProfile() {
        UserProfile profile = UserProfile.builder()
                .userId(1L)
                .displayName("재키")
                .userType(UserType.WOOWACOURSE_CREW)
                .track("BACKEND")
                .cohort((short) 8)
                .build();

        assertThat(profile.getTrack()).isEqualTo("BACKEND");
        assertThat(profile.getCohort()).isEqualTo((short) 8);
    }

    @Test
    @DisplayName("우아한테크코스 코치는 기수를 가질 수 없다")
    void rejectsCohortForWoowacourseCoach() {
        assertThatThrownBy(() -> UserProfile.builder()
                .userId(2L)
                .displayName("상준")
                .userType(UserType.WOOWACOURSE_COACH)
                .track("BACKEND")
                .cohort((short) 8)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("우아한테크코스 코치는 트랙만 가질 수 있다")
    void createsWoowacourseCoachWithTrackOnly() {
        UserProfile profile = UserProfile.builder()
                .userId(2L)
                .displayName("상준")
                .userType(UserType.WOOWACOURSE_COACH)
                .track("BACKEND")
                .build();

        assertThat(profile.getTrack()).isEqualTo("BACKEND");
        assertThat(profile.getCohort()).isNull();
    }
}

package com.shoutoutz.api.user.domain.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
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
        assertThat(profile.getAvatarImageId()).isNull();
        assertThat(profile.getGithubProfileUrl()).isNull();
        assertThat(profile.getBlogUrl()).isNull();
    }

    @Test
    @DisplayName("프로필 생성 시 문자열 표시 이름을 값 객체로 변환한다")
    void convertsRawDisplayNameWhenInitializingUserProfile() {
        assertThatThrownBy(() -> UserProfile.initialize(1L, " "))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("사용자 ID와 표시 이름, 사용자 유형은 필수다")
    void requiresUserIdDisplayNameAndUserType() {
        assertThatThrownBy(() -> UserProfile.builder()
                .displayName("재키")
                .userType(UserType.GENERAL)
                .build())
                .isInstanceOf(DomainValidationException.class);

        assertThatThrownBy(() -> UserProfile.builder()
                .userId(1L)
                .userType(UserType.GENERAL)
                .build())
                .isInstanceOf(DomainValidationException.class);

        assertThatThrownBy(() -> UserProfile.builder()
                .userId(1L)
                .displayName("재키")
                .build())
                .isInstanceOf(DomainValidationException.class);
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
                .isInstanceOf(DomainValidationException.class);
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
                .isInstanceOf(DomainValidationException.class);

        assertThatThrownBy(() -> UserProfile.builder()
                .userId(1L)
                .displayName("재키")
                .userType(UserType.WOOWACOURSE_CREW)
                .cohort((short) 8)
                .build())
                .isInstanceOf(DomainValidationException.class);
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
                .isInstanceOf(DomainValidationException.class);
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

    @Test
    @DisplayName("가입 정보와 GitHub 프로필 주소로 사용자 프로필을 초기화한다")
    void initializesUserProfileWithSignupInformation() {
        UserProfile profile = UserProfile.initialize(
                3L,
                "다혜",
                UserType.WOOWACOURSE_CREW,
                "BACKEND",
                (short) 8,
                "https://github.com/dahye"
        );

        assertThat(profile.getUserId()).isEqualTo(3L);
        assertThat(profile.getDisplayName()).isEqualTo(new ProfileDisplayName("다혜"));
        assertThat(profile.getUserType()).isEqualTo(UserType.WOOWACOURSE_CREW);
        assertThat(profile.getTrack()).isEqualTo("BACKEND");
        assertThat(profile.getCohort()).isEqualTo((short) 8);
        assertThat(profile.getAvatarImageId()).isNull();
        assertThat(profile.getGithubProfileUrl()).isEqualTo("https://github.com/dahye");
    }

    @Test
    @DisplayName("일반 사용자는 표시 이름과 프로필 정보를 수정한다")
    void updatesGeneralUserProfile() {
        UserProfile profile = UserProfile.initialize(1L, "재키");

        UserProfile updated = profile.update(
                "새 이름",
                "소개",
                21L,
                "https://github.com/zzaekkii",
                "https://zzaekkii.dev"
        );

        assertThat(updated.getDisplayName()).isEqualTo(new ProfileDisplayName("새 이름"));
        assertThat(updated.getBio()).isEqualTo("소개");
        assertThat(updated.getAvatarImageId()).isEqualTo(21L);
    }

    @Test
    @DisplayName("우테코 사용자는 표시 이름을 변경할 수 없다")
    void rejectsDisplayNameChangeFromWoowacourseUser() {
        UserProfile profile = UserProfile.initialize(
                1L,
                "재키",
                UserType.WOOWACOURSE_CREW,
                "BACKEND",
                (short) 8,
                null
        );

        assertThatThrownBy(() -> profile.update("새 이름", null, null, null, null))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("한 줄 소개 길이는 유니코드 코드 포인트 기준으로 검증한다")
    void validatesBioLengthByUnicodeCodePoint() {
        UserProfile profile = UserProfile.initialize(1L, "재키");
        String twoHundredEmojis = "😀".repeat(200);
        String twoHundredOneEmojis = "😀".repeat(201);

        assertThat(profile.update("재키", twoHundredEmojis, null, null, null).getBio())
                .isEqualTo(twoHundredEmojis);
        assertThatThrownBy(() -> profile.update("재키", twoHundredOneEmojis, null, null, null))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("프로필 문자열을 정제하고 공백뿐인 선택 값은 null로 변환한다")
    void sanitizesProfileStrings() {
        UserProfile profile = UserProfile.initialize(1L, "재키");

        UserProfile updated = profile.update(
                "  새 이름  ",
                "  소개  ",
                null,
                "   ",
                " https://zzaekkii.dev "
        );

        assertThat(updated.getDisplayName().value()).isEqualTo("새 이름");
        assertThat(updated.getBio()).isEqualTo("소개");
        assertThat(updated.getGithubProfileUrl()).isNull();
        assertThat(updated.getBlogUrl()).isEqualTo("https://zzaekkii.dev");
    }
}

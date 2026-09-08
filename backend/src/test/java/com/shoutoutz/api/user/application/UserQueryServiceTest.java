package com.shoutoutz.api.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.user.application.dto.result.UserProfileResult;
import com.shoutoutz.api.user.application.dto.result.UserProfileSummaryResult;
import com.shoutoutz.api.user.domain.User;
import com.shoutoutz.api.user.domain.UserProfile;
import com.shoutoutz.api.user.domain.UserProfileCounts;
import com.shoutoutz.api.user.domain.UserProfileCountsRepository;
import com.shoutoutz.api.user.domain.UserProfileRepository;
import com.shoutoutz.api.user.domain.UserRepository;
import com.shoutoutz.api.user.domain.UserRole;
import com.shoutoutz.api.user.domain.UserStatus;
import com.shoutoutz.api.user.domain.UserType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserProfileCountsRepository userProfileCountsRepository;

    private UserQueryService userQueryService;

    @BeforeEach
    void setUp() {
        userQueryService = new UserQueryService(
                userRepository,
                userProfileRepository,
                userProfileCountsRepository
        );
    }

    @Test
    @DisplayName("내 프로필 요약을 조회한다")
    void getMyProfileSummary() {
        User user = User.builder()
                .id(1L)
                .handle("zzaekkii")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
        UserProfile profile = UserProfile.builder()
                .userId(1L)
                .displayName("재키")
                .userType(UserType.GENERAL)
                .avatarImageId(21L)
                .build();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));

        UserProfileSummaryResult result = userQueryService.getMyProfileSummary(1L);

        assertThat(result).isEqualTo(new UserProfileSummaryResult(
                1L,
                "zzaekkii",
                "재키",
                21L
        ));
    }

    @Test
    @DisplayName("사용자가 없으면 프로필 요약 조회에 실패한다")
    void failWhenUserDoesNotExist() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userQueryService.getMyProfileSummary(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("사용자 프로필이 없으면 프로필 요약 조회에 실패한다")
    void failWhenUserProfileDoesNotExist() {
        User user = User.builder()
                .id(1L)
                .handle("zzaekkii")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userProfileRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userQueryService.getMyProfileSummary(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("마이페이지 프로필을 조회한다")
    void getMyProfile() {
        User user = User.builder()
                .id(1L)
                .handle("zzaekkii")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
        UserProfile profile = UserProfile.builder()
                .userId(1L)
                .displayName("재키")
                .userType(UserType.WOOWACOURSE_CREW)
                .track("BACKEND")
                .cohort((short) 8)
                .bio("백엔드 개발자입니다.")
                .avatarImageId(21L)
                .githubProfileUrl("https://github.com/zzaekkii")
                .blogUrl("https://zzaekkii.dev")
                .build();
        UserProfileCounts counts = new UserProfileCounts(2L, 18L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));
        given(userProfileCountsRepository.countByUserId(1L)).willReturn(counts);

        UserProfileResult result = userQueryService.getMyProfile(1L);

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.handle()).isEqualTo("zzaekkii");
        assertThat(result.displayName()).isEqualTo("재키");
        assertThat(result.userType()).isEqualTo(UserType.WOOWACOURSE_CREW);
        assertThat(result.track()).isEqualTo("BACKEND");
        assertThat(result.cohort()).isEqualTo((short) 8);
        assertThat(result.bio()).isEqualTo("백엔드 개발자입니다.");
        assertThat(result.avatarImageId()).isEqualTo(21L);
        assertThat(result.githubProfileUrl()).isEqualTo("https://github.com/zzaekkii");
        assertThat(result.blogUrl()).isEqualTo("https://zzaekkii.dev");
        assertThat(result.counts()).isEqualTo(counts);
    }
}

package com.shoutoutz.api.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.user.application.command.UserProfileUpdateCommand;
import com.shoutoutz.api.user.application.command.UserProfileUpdateResult;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.account.UserStatus;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private MediaMetadataRepository mediaMetadataRepository;

    private UserCommandService userCommandService;

    @BeforeEach
    void setUp() {
        userCommandService = new UserCommandService(
                userRepository,
                userProfileRepository,
                mediaMetadataRepository
        );
    }

    @Test
    @DisplayName("일반 사용자의 프로필을 수정한다")
    void updateGeneralUserProfile() {
        User user = user();
        UserProfile profile = generalProfile();
        UserProfileUpdateCommand command = command("새 이름", 21L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));
        given(mediaMetadataRepository.findById(21L))
                .willReturn(Optional.of(media(MediaPurpose.USER_AVATAR, MediaStatus.READY, 1L)));
        given(userProfileRepository.save(org.mockito.ArgumentMatchers.any(UserProfile.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        UserProfileUpdateResult result = userCommandService.updateMyProfile(command);

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.handle()).isEqualTo("zzaekkii");
        assertThat(result.displayName()).isEqualTo("새 이름");
        assertThat(result.avatarImageId()).isEqualTo(21L);
        assertThat(result.bio()).isEqualTo("백엔드 개발자입니다.");
    }

    @Test
    @DisplayName("프로필 이미지 ID가 null이면 기존 이미지를 제거한다")
    void removeAvatarImage() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user()));
        given(userProfileRepository.findByUserId(1L)).willReturn(Optional.of(generalProfile()));
        given(userProfileRepository.save(org.mockito.ArgumentMatchers.any(UserProfile.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        UserProfileUpdateResult result = userCommandService.updateMyProfile(command("재키", null));

        assertThat(result.avatarImageId()).isNull();
        then(mediaMetadataRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("인증된 우테코 사용자는 표시 이름을 변경할 수 없다")
    void rejectDisplayNameChangeFromWoowacourseUser() {
        UserProfile profile = UserProfile.builder()
                .userId(1L)
                .displayName("재키")
                .userType(UserType.WOOWACOURSE_CREW)
                .track("BACKEND")
                .cohort((short) 8)
                .build();
        given(userRepository.findById(1L)).willReturn(Optional.of(user()));
        given(userProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));

        assertThatThrownBy(() -> userCommandService.updateMyProfile(command("새 이름", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("인증된 우아한테크코스 사용자는 표시 이름을 변경할 수 없습니다.");

        then(userProfileRepository).should(never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("다른 사용자가 업로드한 이미지를 프로필 이미지로 사용할 수 없다")
    void rejectAvatarImageUploadedByAnotherUser() {
        givenProfileUpdateTargets();
        given(mediaMetadataRepository.findById(21L))
                .willReturn(Optional.of(media(MediaPurpose.USER_AVATAR, MediaStatus.READY, 2L)));

        assertThatThrownBy(() -> userCommandService.updateMyProfile(command("재키", 21L)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("프로필 이미지 용도가 아닌 미디어를 사용할 수 없다")
    void rejectAvatarImageWithWrongPurpose() {
        givenProfileUpdateTargets();
        given(mediaMetadataRepository.findById(21L))
                .willReturn(Optional.of(media(MediaPurpose.POST_CONTENT, MediaStatus.READY, 1L)));

        assertThatThrownBy(() -> userCommandService.updateMyProfile(command("재키", 21L)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("처리가 완료되지 않은 미디어를 프로필 이미지로 사용할 수 없다")
    void rejectAvatarImageNotReady() {
        givenProfileUpdateTargets();
        given(mediaMetadataRepository.findById(21L))
                .willReturn(Optional.of(media(MediaPurpose.USER_AVATAR, MediaStatus.PROCESSING, 1L)));

        assertThatThrownBy(() -> userCommandService.updateMyProfile(command("재키", 21L)))
                .isInstanceOf(ConflictException.class);
    }

    private void givenProfileUpdateTargets() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user()));
        given(userProfileRepository.findByUserId(1L)).willReturn(Optional.of(generalProfile()));
    }

    private User user() {
        return User.builder()
                .id(1L)
                .handle("zzaekkii")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
    }

    private UserProfile generalProfile() {
        return UserProfile.builder()
                .userId(1L)
                .displayName("재키")
                .userType(UserType.GENERAL)
                .avatarImageId(10L)
                .build();
    }

    private UserProfileUpdateCommand command(String displayName, Long avatarImageId) {
        return new UserProfileUpdateCommand(
                1L,
                displayName,
                "백엔드 개발자입니다.",
                avatarImageId,
                "https://github.com/zzaekkii",
                "https://zzaekkii.dev"
        );
    }

    private MediaMetadata media(MediaPurpose purpose, MediaStatus status, long uploadedBy) {
        Instant now = Instant.parse("2026-09-09T00:00:00Z");
        return MediaMetadata.reconstitute(
                21L,
                uploadedBy,
                purpose,
                "media/avatar/original.png",
                "avatar.png",
                "image/png",
                1_024L,
                status,
                now.plusSeconds(300),
                null,
                status == MediaStatus.READY ? now : null,
                now,
                now
        );
    }
}

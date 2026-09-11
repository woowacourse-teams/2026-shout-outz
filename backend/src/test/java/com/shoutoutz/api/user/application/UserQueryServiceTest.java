package com.shoutoutz.api.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.user.application.dto.result.UserProfileResult;
import com.shoutoutz.api.user.application.dto.result.UserProfileSummaryResult;
import com.shoutoutz.api.user.application.dto.result.UserSearchResult;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.application.query.UserProfileCounts;
import com.shoutoutz.api.user.application.query.UserQueryRepository;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.application.query.UserSearchCursor;
import com.shoutoutz.api.user.application.query.UserSearchItem;
import com.shoutoutz.api.user.domain.account.UserStatus;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserQueryRepository userQueryRepository;

    private UserSearchCursorCodec userSearchCursorCodec;

    private UserQueryService userQueryService;

    @BeforeEach
    void setUp() {
        userSearchCursorCodec = new UserSearchCursorCodec(JsonMapper.builder().build());
        userQueryService = new UserQueryService(
                userRepository,
                userProfileRepository,
                userQueryRepository,
                userSearchCursorCodec
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
        given(userQueryRepository.countByUserId(1L)).willReturn(counts);

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

    @Test
    @DisplayName("handle로 공개 프로필을 조회한다")
    void getPublicProfile() {
        User user = User.builder()
                .id(1L)
                .handle("zzaekkii")
                .status(UserStatus.BANNED)
                .role(UserRole.USER)
                .build();
        UserProfile profile = UserProfile.builder()
                .userId(1L)
                .displayName("재키")
                .userType(UserType.GENERAL)
                .build();
        UserProfileCounts counts = new UserProfileCounts(2L, 18L);
        given(userRepository.findByHandle("zzaekkii")).willReturn(Optional.of(user));
        given(userProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));
        given(userQueryRepository.countByUserId(1L)).willReturn(counts);

        UserProfileResult result = userQueryService.getPublicProfile("zzaekkii");

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.handle()).isEqualTo("zzaekkii");
        assertThat(result.displayName()).isEqualTo("재키");
        assertThat(result.counts()).isEqualTo(counts);
    }

    @Test
    @DisplayName("탈퇴한 사용자의 공개 프로필은 개인정보를 숨긴다")
    void getDeletedUserPublicProfile() {
        User user = User.builder()
                .id(1L)
                .handle("zzaekkii")
                .status(UserStatus.DELETED)
                .role(UserRole.USER)
                .deletedAt(Instant.now())
                .build();
        given(userRepository.findByHandle("zzaekkii")).willReturn(Optional.of(user));

        UserProfileResult result = userQueryService.getPublicProfile("zzaekkii");

        assertThat(result.displayName()).isEqualTo("탈퇴한 사용자");
        assertThat(result.userType()).isNull();
        assertThat(result.track()).isNull();
        assertThat(result.cohort()).isNull();
        assertThat(result.bio()).isNull();
        assertThat(result.avatarImageId()).isNull();
        assertThat(result.githubProfileUrl()).isNull();
        assertThat(result.blogUrl()).isNull();
        assertThat(result.counts()).isEqualTo(new UserProfileCounts(0L, 0L));
        then(userProfileRepository).should(never()).findByUserId(1L);
        then(userQueryRepository).should(never()).countByUserId(1L);
    }

    @Test
    @DisplayName("프로젝트 참여자를 검색하고 다음 커서를 생성한다")
    void searchProjectMember() {
        List<UserSearchItem> searchedItems = List.of(
                searchItem("dahye", "다혜", 2),
                searchItem("hoi", "호이", 2),
                searchItem("charles", "샤를", 2)
        );
        given(userQueryRepository.searchProjectMember("재", null, 3))
                .willReturn(searchedItems);

        UserSearchResult result = userQueryService.searchProjectMember("재", null, 2);

        assertThat(result.items()).containsExactly(searchedItems.get(0), searchedItems.get(1));
        assertThat(userSearchCursorCodec.decode(result.nextCursor()))
                .isEqualTo(new UserSearchCursor(2, "호이", "hoi"));
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("커서를 해석해 다음 프로젝트 참여자를 검색한다")
    void searchProjectMemberWithCursor() {
        UserSearchCursor cursor = new UserSearchCursor(1, "재키", "zzaekkii");
        given(userQueryRepository.searchProjectMember("재키", cursor, 21))
                .willReturn(List.of());

        UserSearchResult result = userQueryService.searchProjectMember(
                "재키",
                userSearchCursorCodec.encode(cursor),
                20
        );

        assertThat(result.items()).isEmpty();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("형식이 잘못된 검색 커서를 거절한다")
    void rejectInvalidSearchCursor() {
        assertThatThrownBy(() -> userQueryService.searchProjectMember("재키", "invalid", 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 커서입니다.");

        then(userQueryRepository).shouldHaveNoInteractions();
    }

    private UserSearchItem searchItem(
            String handle,
            String displayName,
            int relevanceRank
    ) {
        return new UserSearchItem(
                handle,
                displayName,
                UserType.WOOWACOURSE_CREW,
                "BACKEND",
                (short) 8,
                null,
                relevanceRank
        );
    }
}

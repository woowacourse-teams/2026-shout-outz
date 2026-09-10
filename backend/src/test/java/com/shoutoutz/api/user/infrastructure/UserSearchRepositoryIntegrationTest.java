package com.shoutoutz.api.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.application.query.UserSearchCursor;
import com.shoutoutz.api.user.application.query.UserSearchItem;
import com.shoutoutz.api.user.application.query.UserQueryRepository;
import com.shoutoutz.api.user.domain.account.UserStatus;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.user.infrastructure.jpa.UserProfileJpaRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UserSearchRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserQueryRepository userQueryRepository;

    @Autowired
    private UserProfileJpaRepository userProfileJpaRepository;

    @Test
    @DisplayName("본인을 포함해 이름이나 handle이 일치하는 ACTIVE 크루를 관련도순으로 검색한다")
    void searchCrew() {
        User requester = saveUser("jack-requester", UserStatus.ACTIVE);
        saveProfile(requester.getId(), "Jack Owner", UserType.WOOWACOURSE_CREW);
        User exactlyMatched = saveUser("dahye", UserStatus.ACTIVE);
        saveProfile(exactlyMatched.getId(), "Jack", UserType.WOOWACOURSE_CREW);
        User prefixMatched = saveUser("jack-dev", UserStatus.ACTIVE);
        saveProfile(prefixMatched.getId(), "Jack Zebra", UserType.WOOWACOURSE_CREW);
        User containsMatched = saveUser("my-jack-dev", UserStatus.ACTIVE);
        saveProfile(containsMatched.getId(), "다른 크루", UserType.WOOWACOURSE_CREW);
        User general = saveUser("jack-general", UserStatus.ACTIVE);
        saveProfile(general.getId(), "Jack General", UserType.GENERAL);
        User banned = saveUser("jack-banned", UserStatus.BANNED);
        saveProfile(banned.getId(), "Jack Banned", UserType.WOOWACOURSE_CREW);
        userProfileJpaRepository.flush();

        List<UserSearchItem> result = userQueryRepository.searchCrew(
                "jack",
                null,
                10
        );

        assertThat(result).extracting(UserSearchItem::handle)
                .containsExactly(
                        exactlyMatched.getHandle().value(),
                        requester.getHandle().value(),
                        prefixMatched.getHandle().value(),
                        containsMatched.getHandle().value()
                );
        assertThat(result).extracting(UserSearchItem::relevanceRank)
                .containsExactly(0, 1, 1, 2);
    }

    @Test
    @DisplayName("커서의 정렬 키 다음에 위치한 크루만 검색한다")
    void searchCrewAfterCursor() {
        User requester = saveUser("cursor-requester", UserStatus.ACTIVE);
        saveProfile(requester.getId(), "요청자", UserType.WOOWACOURSE_CREW);
        User first = saveUser("cursor-jack-one", UserStatus.ACTIVE);
        saveProfile(first.getId(), "가 크루", UserType.WOOWACOURSE_CREW);
        User second = saveUser("cursor-jack-two", UserStatus.ACTIVE);
        saveProfile(second.getId(), "나 크루", UserType.WOOWACOURSE_CREW);
        userProfileJpaRepository.flush();

        List<UserSearchItem> firstSlice = userQueryRepository.searchCrew(
                "jack",
                null,
                10
        );
        UserSearchItem cursorItem = firstSlice.getFirst();
        UserSearchCursor cursor = new UserSearchCursor(
                cursorItem.relevanceRank(),
                cursorItem.displayName(),
                cursorItem.handle()
        );

        List<UserSearchItem> result = userQueryRepository.searchCrew(
                "jack",
                cursor,
                10
        );

        assertThat(result).extracting(UserSearchItem::handle)
                .containsExactly(firstSlice.getLast().handle());
    }

    @Test
    @DisplayName("LIKE 와일드카드를 일반 검색 문자로 취급한다")
    void escapeLikeWildcard() {
        User requester = saveUser("wildcard-requester", UserStatus.ACTIVE);
        saveProfile(requester.getId(), "요청자", UserType.WOOWACOURSE_CREW);
        User matched = saveUser("wildcard-matched", UserStatus.ACTIVE);
        saveProfile(matched.getId(), "성장률 100%", UserType.WOOWACOURSE_CREW);
        User unmatched = saveUser("wildcard-unmatched", UserStatus.ACTIVE);
        saveProfile(unmatched.getId(), "일반 크루", UserType.WOOWACOURSE_CREW);
        userProfileJpaRepository.flush();

        List<UserSearchItem> result = userQueryRepository.searchCrew(
                "%",
                null,
                10
        );

        assertThat(result).extracting(UserSearchItem::handle)
                .containsExactly(matched.getHandle().value());
    }

    private User saveUser(String handle, UserStatus status) {
        return userRepository.save(User.builder()
                .handle(handle)
                .status(status)
                .role(UserRole.USER)
                .build());
    }

    private void saveProfile(long userId, String displayName, UserType userType) {
        UserProfile.UserProfileBuilder profileBuilder = UserProfile.builder()
                .userId(userId)
                .displayName(displayName)
                .userType(userType);
        if (userType == UserType.WOOWACOURSE_CREW) {
            profileBuilder.track("BACKEND").cohort((short) 8);
        }
        userProfileRepository.save(profileBuilder.build());
    }
}

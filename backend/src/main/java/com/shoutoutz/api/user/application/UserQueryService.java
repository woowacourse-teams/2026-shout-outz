package com.shoutoutz.api.user.application;

import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.user.application.dto.result.UserProfileSummaryResult;
import com.shoutoutz.api.user.application.dto.result.UserProfileResult;
import com.shoutoutz.api.user.application.dto.result.UserSearchResult;
import com.shoutoutz.api.user.domain.account.Handle;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.application.query.UserProfileCounts;
import com.shoutoutz.api.user.application.query.UserQueryRepository;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.application.query.UserSearchCursor;
import com.shoutoutz.api.user.application.query.UserSearchItem;
import com.shoutoutz.api.user.domain.account.UserStatus;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.user.exception.UserErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private static final String DELETED_USER_DISPLAY_NAME = "탈퇴한 사용자";
    private static final int MAX_SEARCH_KEYWORD_LENGTH = 50;
    private static final int MAX_SEARCH_SIZE = 100;

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserQueryRepository userQueryRepository;
    private final UserSearchCursorCodec userSearchCursorCodec;

    @Transactional(readOnly = true)
    public UserProfileSummaryResult getMyProfileSummary(long userId) {
        User user = findUser(userId);
        UserProfile profile = findProfile(userId);

        return new UserProfileSummaryResult(
                user.getId(),
                user.getHandle().value(),
                profile.getDisplayName().value(),
                profile.getAvatarImageId()
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResult getMyProfile(long userId) {
        User user = findUser(userId);
        UserProfile profile = findProfile(userId);
        UserProfileCounts counts = userQueryRepository.countByUserId(userId);

        return profileResult(user, profile, counts);
    }

    @Transactional(readOnly = true)
    public UserProfileResult getPublicProfile(String handle) {
        String validatedHandle = new Handle(handle).value();
        User user = userRepository.findByHandle(validatedHandle)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.DELETED) {
            return deletedProfile(user);
        }

        UserProfile profile = findProfile(user.getId());
        UserProfileCounts counts = userQueryRepository.countByUserId(user.getId());
        return profileResult(user, profile, counts);
    }

    @Transactional(readOnly = true)
    public UserSearchResult searchCrew(
            long requesterId,
            String keyword,
            String cursor,
            int size
    ) {
        validateCrewRequester(requesterId);
        String validatedKeyword = validateSearchKeyword(keyword);
        validateSearchSize(size);
        UserSearchCursor decodedCursor = userSearchCursorCodec.decode(cursor);

        List<UserSearchItem> searchedItems = userQueryRepository.searchCrew(
                validatedKeyword,
                decodedCursor,
                size + 1
        );
        boolean hasNext = searchedItems.size() > size;
        List<UserSearchItem> items = hasNext
                ? List.copyOf(searchedItems.subList(0, size))
                : List.copyOf(searchedItems);
        String nextCursor = hasNext ? encodeCursor(items.getLast()) : null;

        return new UserSearchResult(items, nextCursor, hasNext);
    }

    private void validateCrewRequester(long requesterId) {
        UserProfile requesterProfile = findProfile(requesterId);
        if (requesterProfile.getUserType() != UserType.WOOWACOURSE_CREW) {
            throw new ForbiddenException(CommonErrorCode.FORBIDDEN);
        }
    }

    private String validateSearchKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("검색어는 필수입니다.");
        }

        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.codePointCount(0, trimmedKeyword.length()) > MAX_SEARCH_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("검색어는 50자를 초과할 수 없습니다.");
        }
        return trimmedKeyword;
    }

    private void validateSearchSize(int size) {
        if (size < 1 || size > MAX_SEARCH_SIZE) {
            throw new IllegalArgumentException("검색 결과 개수는 1개 이상 100개 이하여야 합니다.");
        }
    }

    private String encodeCursor(UserSearchItem item) {
        return userSearchCursorCodec.encode(new UserSearchCursor(
                item.relevanceRank(),
                item.displayName(),
                item.handle()
        ));
    }

    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_NOT_FOUND));
    }

    private UserProfile findProfile(long userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_PROFILE_NOT_FOUND));
    }

    private UserProfileResult profileResult(
            User user,
            UserProfile profile,
            UserProfileCounts counts
    ) {
        return new UserProfileResult(
                user.getId(),
                user.getHandle().value(),
                profile.getDisplayName().value(),
                profile.getUserType(),
                profile.getTrack(),
                profile.getCohort(),
                profile.getBio(),
                profile.getAvatarImageId(),
                profile.getGithubProfileUrl(),
                profile.getBlogUrl(),
                counts
        );
    }

    private UserProfileResult deletedProfile(User user) {
        return new UserProfileResult(
                user.getId(),
                user.getHandle().value(),
                DELETED_USER_DISPLAY_NAME,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new UserProfileCounts(0L, 0L)
        );
    }
}

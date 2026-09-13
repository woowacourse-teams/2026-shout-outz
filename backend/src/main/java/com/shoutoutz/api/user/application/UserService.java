package com.shoutoutz.api.user.application;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.user.application.dto.UserProfileCounts;
import com.shoutoutz.api.user.application.dto.UserSearchCursor;
import com.shoutoutz.api.user.application.dto.UserSearchItem;
import com.shoutoutz.api.user.application.dto.UserSearchResult;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.exception.UserErrorCode;
import com.shoutoutz.api.user.presentation.dto.request.UserProfileUpdateRequest;
import com.shoutoutz.api.user.presentation.dto.response.UserProfileResponse;
import com.shoutoutz.api.user.presentation.dto.response.UserProfileSummaryResponse;
import com.shoutoutz.api.user.presentation.dto.response.UserProfileUpdateResponse;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 프로필 조회·수정과 우테코 사용자 검색 유스케이스를 처리한다. */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DELETED_USER_DISPLAY_NAME = "탈퇴한 사용자";
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserQueryRepository userQueryRepository;
    private final UserSearchCursorCodec userSearchCursorCodec;
    private final MediaMetadataRepository mediaMetadataRepository;

    @Transactional
    public UserProfileUpdateResponse updateMyProfile(
            long userId,
            UserProfileUpdateRequest request
    ) {
        User user = findUser(userId);
        UserProfile profile = findProfile(userId);

        if (!profile.canChangeDisplayNameTo(request.displayName())) {
            throw new BadRequestException(UserErrorCode.PROFILE_DISPLAY_NAME_IMMUTABLE);
        }
        validateAvatarImage(userId, request.avatarImageId());

        UserProfile updatedProfile = profile.update(
                request.displayName(),
                request.bio(),
                request.avatarImageId(),
                request.githubProfileUrl(),
                request.blogUrl()
        );
        UserProfile savedProfile = userProfileRepository.save(updatedProfile);

        return new UserProfileUpdateResponse(
                user.getHandle().value(),
                savedProfile.getDisplayName().value(),
                savedProfile.getUserType(),
                savedProfile.getTrack(),
                savedProfile.getCohort(),
                savedProfile.getBio(),
                savedProfile.getAvatarImageId(),
                savedProfile.getGithubProfileUrl(),
                savedProfile.getBlogUrl()
        );
    }

    @Transactional(readOnly = true)
    public UserProfileSummaryResponse getMyProfileSummary(long userId) {
        User user = findUser(userId);
        UserProfile profile = findProfile(userId);

        return new UserProfileSummaryResponse(
                user.getHandle().value(),
                profile.getDisplayName().value(),
                profile.getAvatarImageId()
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(long userId) {
        User user = findUser(userId);
        UserProfile profile = findProfile(userId);
        UserProfileCounts counts = userQueryRepository.countByUserId(userId);

        return createProfileResponse(user, profile, counts);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getPublicProfile(String handle) {
        User user = userRepository.findByHandle(handle)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            return createDeletedProfileResponse(user);
        }

        UserProfile profile = findProfile(user.getId());
        UserProfileCounts counts = userQueryRepository.countByUserId(user.getId());
        return createProfileResponse(user, profile, counts);
    }

    @Transactional(readOnly = true)
    public UserSearchResult searchWoowaUsers(
            String keyword,
            String cursor,
            int size
    ) {
        UserSearchCursor decodedCursor = userSearchCursorCodec.decode(cursor);

        List<UserSearchItem> searchedItems = userQueryRepository.searchWoowaUsers(
                keyword,
                decodedCursor,
                size + 1
        );

        return createSearchResult(searchedItems, size);
    }

    /** 한 건을 더 조회한 결과로 다음 검색 여부와 커서를 결정한다. */
    private UserSearchResult createSearchResult(
            List<UserSearchItem> searchedItems,
            int size
    ) {
        if (searchedItems.size() <= size) {
            return new UserSearchResult(List.copyOf(searchedItems), null, false);
        }

        List<UserSearchItem> items = List.copyOf(searchedItems.subList(0, size));
        return new UserSearchResult(items, encodeCursor(items.getLast()), true);
    }

    private String encodeCursor(UserSearchItem item) {
        return userSearchCursorCodec.encode(new UserSearchCursor(
                item.relevanceRank(),
                item.displayName(),
                item.handle()
        ));
    }

    /** 프로필 이미지의 소유자, 용도, 처리 완료 상태를 확인한다. */
    private void validateAvatarImage(long userId, Long avatarImageId) {
        if (avatarImageId == null) {
            return;
        }

        MediaMetadata metadata = mediaMetadataRepository.findById(avatarImageId)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.AVATAR_IMAGE_NOT_FOUND));
        if (!Objects.equals(metadata.getUploadedBy(), userId)) {
            throw new ForbiddenException(UserErrorCode.AVATAR_IMAGE_FORBIDDEN);
        }
        if (metadata.getPurpose() != MediaPurpose.USER_AVATAR) {
            throw new BadRequestException(UserErrorCode.AVATAR_IMAGE_INVALID_PURPOSE);
        }
        if (metadata.getStatus() != MediaStatus.READY) {
            throw new ConflictException(UserErrorCode.AVATAR_IMAGE_NOT_READY);
        }
    }

    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_NOT_FOUND));
    }

    private UserProfile findProfile(long userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_PROFILE_NOT_FOUND));
    }

    private UserProfileResponse createProfileResponse(
            User user,
            UserProfile profile,
            UserProfileCounts counts
    ) {
        return new UserProfileResponse(
                user.getHandle().value(),
                profile.getDisplayName().value(),
                profile.getUserType(),
                profile.getTrack(),
                profile.getCohort(),
                profile.getBio(),
                profile.getAvatarImageId(),
                profile.getGithubProfileUrl(),
                profile.getBlogUrl(),
                new UserProfileResponse.Counts(counts.projects(), counts.posts())
        );
    }

    /** 탈퇴 사용자의 개인정보와 활동 개수를 숨긴 공개 응답을 만든다. */
    private UserProfileResponse createDeletedProfileResponse(User user) {
        return new UserProfileResponse(
                user.getHandle().value(),
                DELETED_USER_DISPLAY_NAME,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new UserProfileResponse.Counts(0L, 0L)
        );
    }
}

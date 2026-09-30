package com.shoutoutz.api.user.application;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.auth.domain.OAuthAccountRepository;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.media.application.MediaUrlResolver;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.user.application.dto.UserProfileCounts;
import com.shoutoutz.api.user.application.dto.UserSearchCursor;
import com.shoutoutz.api.user.application.dto.UserSearchItem;
import com.shoutoutz.api.user.application.dto.UserSearchPage;
import com.shoutoutz.api.user.application.dto.UserSearchResult;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserErrorCode;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileErrorCode;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.Track;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.user.presentation.dto.request.UserProfileUpdateRequest;
import com.shoutoutz.api.user.presentation.dto.response.UserProfileResponse;
import com.shoutoutz.api.user.presentation.dto.response.UserProfileSummaryResponse;
import com.shoutoutz.api.user.presentation.dto.response.UserProfileUpdateResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 프로필 조회, 수정 및 우테코 사용자 검색 서비스.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DELETED_USER_DISPLAY_NAME = "탈퇴한 사용자";
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserQueryRepository userQueryRepository;
    private final UserSearchCursorCodec userSearchCursorCodec;
    private final MediaMetadataRepository mediaMetadataRepository;
    private final MediaUrlResolver mediaUrlResolver;
    private final OAuthAccountRepository oauthAccountRepository;

    @Transactional
    public UserProfileUpdateResponse updateMyProfile(
            long userId,
            UserProfileUpdateRequest request
    ) {
        User user = findUser(userId);
        UserProfile profile = findProfile(userId);

        UserProfile updatedProfile = profile.update(
                request.displayName(),
                request.bio(),
                request.avatarImageId(),
                request.githubProfileUrl(),
                request.blogUrl()
        );
        validateAvatarImage(userId, request.avatarImageId());
        UserProfile savedProfile = userProfileRepository.save(updatedProfile);

        return new UserProfileUpdateResponse(
                user.getId(),
                user.getHandle().value(),
                savedProfile.getDisplayName().value(),
                savedProfile.getUserType(),
                trackValue(savedProfile),
                cohortValue(savedProfile),
                savedProfile.getBio(),
                resolveAvatarUrl(userId, savedProfile.getAvatarImageId()),
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
                profile.getAvatarImageId(),
                resolveAvatarUrl(userId, profile.getAvatarImageId())
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(long userId) {
        User user = findUser(userId);
        UserProfile profile = findProfile(userId);
        UserProfileCounts counts = userQueryRepository.countByUserId(userId, true);

        return createProfileResponse(user, profile, counts);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getPublicProfile(String handle) {
        return getPublicProfile(handle, null);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getPublicProfile(String handle, Long viewerId) {
        User user = userRepository.findByHandle(handle)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            return createDeletedProfileResponse(user);
        }

        UserProfile profile = findProfile(user.getId());
        UserProfileCounts counts = userQueryRepository.countByUserId(
                user.getId(),
                Objects.equals(user.getId(), viewerId)
        );
        return createProfileResponse(user, profile, counts);
    }

    @Transactional(readOnly = true)
    public UserSearchResult searchWoowaMember(
            String keyword,
            String cursor,
            int size
    ) {
        UserSearchCursor decodedCursor = userSearchCursorCodec.decode(cursor);

        UserSearchPage page = userQueryRepository.searchWoowaMember(
                keyword,
                decodedCursor,
                size
        );

        return createSearchResult(page);
    }

    /**
     * 조회 페이지의 다음 페이지 여부로 커서를 생성한다.
     */
    private UserSearchResult createSearchResult(UserSearchPage page) {
        List<UserSearchItem> items = page.items();
        if (!page.hasNext()) {
            return new UserSearchResult(items, null, false, page.totalCount(), resolveAvatarUrls(items));
        }

        return new UserSearchResult(
                items,
                encodeCursor(items.getLast()),
                true,
                page.totalCount(),
                resolveAvatarUrls(items)
        );
    }

    private String encodeCursor(UserSearchItem item) {
        return userSearchCursorCodec.encode(new UserSearchCursor(
                item.relevanceRank(),
                item.displayName(),
                item.handle()
        ));
    }

    /**
     * 프로필 이미지 소유자, 용도 및 처리 상태 검증.
     */
    private void validateAvatarImage(long userId, Long avatarImageId) {
        if (avatarImageId == null) {
            return;
        }

        MediaMetadata metadata = mediaMetadataRepository.findById(avatarImageId)
                .orElseThrow(() -> new EntityNotFoundException(UserProfileErrorCode.AVATAR_IMAGE_NOT_FOUND));
        if (!Objects.equals(metadata.getUploadedBy(), userId)) {
            throw new ForbiddenException(UserProfileErrorCode.AVATAR_IMAGE_FORBIDDEN);
        }
        if (metadata.getPurpose() != MediaPurpose.USER_AVATAR) {
            throw new BadRequestException(UserProfileErrorCode.AVATAR_IMAGE_INVALID_PURPOSE);
        }
        if (metadata.getStatus() != MediaStatus.READY) {
            throw new ConflictException(UserProfileErrorCode.AVATAR_IMAGE_NOT_READY);
        }
    }

    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_NOT_FOUND));
    }

    private UserProfile findProfile(long userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(UserProfileErrorCode.USER_PROFILE_NOT_FOUND));
    }

    private UserProfileResponse createProfileResponse(
            User user,
            UserProfile profile,
            UserProfileCounts counts
    ) {
        return new UserProfileResponse(
                user.getId(),
                user.getHandle().value(),
                profile.getDisplayName().value(),
                profile.getUserType(),
                trackValue(profile),
                cohortValue(profile),
                profile.getBio(),
                profile.getAvatarImageId(),
                resolveAvatarUrl(profile.getUserId(), profile.getAvatarImageId()),
                profile.getGithubProfileUrl(),
                profile.getBlogUrl(),
                new UserProfileResponse.Counts(counts.projects(), counts.feeds())
        );
    }

    private String trackValue(UserProfile profile) {
        if (profile.getUserType() != UserType.WOOWACOURSE_CREW) {
            return null;
        }
        Track track = profile.getTrack();
        if (track == null) {
            return null;
        }
        return track.getValue();
    }

    private Short cohortValue(UserProfile profile) {
        if (profile.getUserType() != UserType.WOOWACOURSE_CREW) {
            return null;
        }
        Cohort cohort = profile.getCohort();
        if (cohort == null) {
            return null;
        }
        return (short) cohort.getValue();
    }

    private Map<Long, java.net.URI> resolveAvatarUrls(List<UserSearchItem> items) {
        Map<Long, java.net.URI> urls = mediaUrlResolver.resolveAll(items.stream()
                .map(UserSearchItem::avatarImageId)
                .filter(Objects::nonNull)
                .toList());
        return urls == null ? Map.of() : urls;
    }

    private String toUrl(java.net.URI url) {
        return url == null ? null : url.toString();
    }

    /**
     * 사용자가 직접 업로드한 이미지가 있으면 그 이미지를 우선하고,
     * 아직 업로드하지 않은 신규 OAuth 사용자는 Provider 아바타를 사용한다.
     *
     * <p>{@code avatarImageId}는 {@code media_metadata.id}를 가리키는 값이므로
     * 외부 GitHub URL을 이 필드에 저장하지 않는다.</p>
     */
    private String resolveAvatarUrl(long userId, Long avatarImageId) {
        String mediaUrl = toUrl(mediaUrlResolver.resolve(avatarImageId));
        if (mediaUrl != null) {
            return mediaUrl;
        }
        return oauthAccountRepository.findByUserIdAndProvider(userId, OAuthProvider.GITHUB)
                .map(account -> account.getProviderAvatarUrl())
                .orElse(null);
    }

    /**
     * 탈퇴 사용자의 개인정보와 활동 개수를 숨긴 공개 프로필 응답 생성.
     */
    private UserProfileResponse createDeletedProfileResponse(User user) {
        return new UserProfileResponse(
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
                null,
                new UserProfileResponse.Counts(0L, 0L)
        );
    }
}

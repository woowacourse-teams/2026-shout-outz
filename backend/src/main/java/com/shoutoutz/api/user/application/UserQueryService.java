package com.shoutoutz.api.user.application;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.user.application.dto.result.UserProfileSummaryResult;
import com.shoutoutz.api.user.application.dto.result.UserProfileResult;
import com.shoutoutz.api.user.domain.Handle;
import com.shoutoutz.api.user.domain.User;
import com.shoutoutz.api.user.domain.UserProfile;
import com.shoutoutz.api.user.domain.UserProfileCounts;
import com.shoutoutz.api.user.domain.UserProfileCountsRepository;
import com.shoutoutz.api.user.domain.UserProfileRepository;
import com.shoutoutz.api.user.domain.UserRepository;
import com.shoutoutz.api.user.domain.UserStatus;
import com.shoutoutz.api.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private static final String DELETED_USER_DISPLAY_NAME = "탈퇴한 사용자";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileCountsRepository userProfileCountsRepository;

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
        UserProfileCounts counts = userProfileCountsRepository.countByUserId(userId);

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
        UserProfileCounts counts = userProfileCountsRepository.countByUserId(user.getId());
        return profileResult(user, profile, counts);
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

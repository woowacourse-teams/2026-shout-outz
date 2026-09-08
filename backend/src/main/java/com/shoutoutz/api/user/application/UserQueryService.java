package com.shoutoutz.api.user.application;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.user.application.dto.result.UserProfileSummaryResult;
import com.shoutoutz.api.user.application.dto.result.UserProfileResult;
import com.shoutoutz.api.user.domain.User;
import com.shoutoutz.api.user.domain.UserProfile;
import com.shoutoutz.api.user.domain.UserProfileCounts;
import com.shoutoutz.api.user.domain.UserProfileCountsRepository;
import com.shoutoutz.api.user.domain.UserProfileRepository;
import com.shoutoutz.api.user.domain.UserRepository;
import com.shoutoutz.api.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQueryService {

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

    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_NOT_FOUND));
    }

    private UserProfile findProfile(long userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_PROFILE_NOT_FOUND));
    }
}

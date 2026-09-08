package com.shoutoutz.api.user.application.dto.result;

import com.shoutoutz.api.user.domain.UserProfileCounts;
import com.shoutoutz.api.user.domain.UserType;

public record UserProfileResult(
        Long userId,
        String handle,
        String displayName,
        UserType userType,
        String track,
        Short cohort,
        String bio,
        Long avatarImageId,
        String githubProfileUrl,
        String blogUrl,
        UserProfileCounts counts
) {
}

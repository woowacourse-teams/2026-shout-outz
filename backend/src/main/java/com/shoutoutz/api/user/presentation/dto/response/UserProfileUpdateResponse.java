package com.shoutoutz.api.user.presentation.dto.response;

import com.shoutoutz.api.user.domain.profile.UserType;

public record UserProfileUpdateResponse(
        Long userId,
        String handle,
        String displayName,
        UserType userType,
        String track,
        Short cohort,
        String bio,
        String avatarUrl,
        String githubProfileUrl,
        String blogUrl
) {
    public UserProfileUpdateResponse(
            String handle,
            String displayName,
            UserType userType,
            String track,
            Short cohort,
            String bio,
            String avatarUrl,
            String githubProfileUrl,
            String blogUrl
    ) {
        this(null, handle, displayName, userType, track, cohort, bio, avatarUrl, githubProfileUrl, blogUrl);
    }
}

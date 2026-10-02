package com.shoutoutz.api.user.presentation.dto.response;

import com.shoutoutz.api.user.domain.profile.UserType;

public record UserProfileResponse(
        Long userId,
        String handle,
        String displayName,
        UserType userType,
        String track,
        Short cohort,
        String bio,
        Long avatarImageId,
        String avatarUrl,
        String githubProfileUrl,
        String blogUrl,
        Counts counts
) {
    public UserProfileResponse(
            String handle,
            String displayName,
            UserType userType,
            String track,
            Short cohort,
            String bio,
            Long avatarImageId,
            String avatarUrl,
            String githubProfileUrl,
            String blogUrl,
            Counts counts
    ) {
        this(null, handle, displayName, userType, track, cohort, bio, avatarImageId, avatarUrl,
                githubProfileUrl, blogUrl, counts);
    }

    public record Counts(
            long projects,
            long feeds
    ) {
    }
}

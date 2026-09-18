package com.shoutoutz.api.user.presentation.dto.response;

import com.shoutoutz.api.user.domain.profile.UserType;

public record UserProfileResponse(
        String handle,
        String displayName,
        UserType userType,
        String track,
        Short cohort,
        String bio,
        String avatarUrl,
        String githubProfileUrl,
        String blogUrl,
        Counts counts
) {
    public record Counts(
            long projects,
            long feeds
    ) {
    }
}

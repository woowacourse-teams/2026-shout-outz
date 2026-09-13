package com.shoutoutz.api.user.presentation.dto.response;

import com.shoutoutz.api.user.domain.profile.UserType;

public record UserProfileUpdateResponse(
        String handle,
        String displayName,
        UserType userType,
        String track,
        Short cohort,
        String bio,
        Long avatarImageId,
        String githubProfileUrl,
        String blogUrl
) {
}

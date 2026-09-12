package com.shoutoutz.api.user.presentation.dto.response;

import com.shoutoutz.api.user.application.command.UserProfileUpdateResult;
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

    public static UserProfileUpdateResponse from(UserProfileUpdateResult result) {
        return new UserProfileUpdateResponse(
                result.handle(),
                result.displayName(),
                result.userType(),
                result.track(),
                result.cohort(),
                result.bio(),
                result.avatarImageId(),
                result.githubProfileUrl(),
                result.blogUrl()
        );
    }
}

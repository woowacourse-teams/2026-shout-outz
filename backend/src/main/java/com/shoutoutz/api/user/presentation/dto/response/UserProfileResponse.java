package com.shoutoutz.api.user.presentation.dto.response;

import com.shoutoutz.api.user.application.dto.result.UserProfileResult;
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
        String githubProfileUrl,
        String blogUrl,
        Counts counts
) {

    public static UserProfileResponse from(UserProfileResult result) {
        return new UserProfileResponse(
                result.userId(),
                result.handle(),
                result.displayName(),
                result.userType(),
                result.track(),
                result.cohort(),
                result.bio(),
                result.avatarImageId(),
                result.githubProfileUrl(),
                result.blogUrl(),
                new Counts(result.counts().projects(), result.counts().posts())
        );
    }

    public record Counts(
            long projects,
            long posts
    ) {
    }
}

package com.shoutoutz.api.user.application.query;

import com.shoutoutz.api.user.domain.profile.UserType;

public record UserSearchItem(
        String handle,
        String displayName,
        UserType userType,
        String track,
        Short cohort,
        Long avatarImageId,
        int relevanceRank
) {
}

package com.shoutoutz.api.user.domain;

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

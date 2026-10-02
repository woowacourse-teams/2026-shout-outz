package com.shoutoutz.api.user.application.dto;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.user.domain.profile.Track;
import com.shoutoutz.api.user.domain.profile.UserType;

public record UserSearchItem(
        Long userId,
        String handle,
        String displayName,
        UserType userType,
        Track track,
        Cohort cohort,
        Long avatarImageId,
        int relevanceRank
) {
    public UserSearchItem(
            String handle,
            String displayName,
            UserType userType,
            Track track,
            Cohort cohort,
            Long avatarImageId,
            int relevanceRank
    ) {
        this(null, handle, displayName, userType, track, cohort, avatarImageId, relevanceRank);
    }
}

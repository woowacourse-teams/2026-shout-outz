package com.shoutoutz.api.comment.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserType;

public record MagazineCommentAuthorResponse(
        Long userId,
        String handle,
        String displayName,
        UserType userType,
        String track,
        @JsonInclude(JsonInclude.Include.NON_NULL) Short cohort,
        Boolean isCurrent,
        String avatarUrl
) {
    public static MagazineCommentAuthorResponse from(UserProfile profile, String handle, String avatarUrl) {
        String track = null;
        Short cohort = null;
        Boolean isCurrent = null;
        if (profile.getUserType() == UserType.WOOWACOURSE_CREW) {
            if (profile.getTrack() != null) {
                track = profile.getTrack().getValue();
            }
            if (profile.getCohort() != null) {
                cohort = (short) profile.getCohort().getValue();
                isCurrent = profile.getCohort() == Cohort.current();
            }
        }
        return new MagazineCommentAuthorResponse(
                profile.getUserId(), handle, profile.getDisplayName().value(),
                profile.getUserType(), track, cohort, isCurrent, avatarUrl
        );
    }
}

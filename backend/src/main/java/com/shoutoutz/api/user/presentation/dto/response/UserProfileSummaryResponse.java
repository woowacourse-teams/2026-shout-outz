package com.shoutoutz.api.user.presentation.dto.response;

import com.shoutoutz.api.user.application.query.UserProfileSummaryResult;

public record UserProfileSummaryResponse(
        String handle,
        String displayName,
        Long avatarImageId
) {

    public static UserProfileSummaryResponse from(UserProfileSummaryResult result) {
        return new UserProfileSummaryResponse(
                result.handle(),
                result.displayName(),
                result.avatarImageId()
        );
    }
}

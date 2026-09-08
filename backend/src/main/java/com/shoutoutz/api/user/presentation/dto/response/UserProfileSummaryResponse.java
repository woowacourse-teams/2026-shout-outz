package com.shoutoutz.api.user.presentation.dto.response;

import com.shoutoutz.api.user.application.dto.result.UserProfileSummaryResult;

public record UserProfileSummaryResponse(
        Long userId,
        String handle,
        String displayName,
        Long avatarImageId
) {

    public static UserProfileSummaryResponse from(UserProfileSummaryResult result) {
        return new UserProfileSummaryResponse(
                result.userId(),
                result.handle(),
                result.displayName(),
                result.avatarImageId()
        );
    }
}

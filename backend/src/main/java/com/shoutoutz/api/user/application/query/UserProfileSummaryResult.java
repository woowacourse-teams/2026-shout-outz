package com.shoutoutz.api.user.application.query;

public record UserProfileSummaryResult(
        Long userId,
        String handle,
        String displayName,
        Long avatarImageId
) {
}

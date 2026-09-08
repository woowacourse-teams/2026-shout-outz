package com.shoutoutz.api.user.application.dto.result;

public record UserProfileSummaryResult(
        Long userId,
        String handle,
        String displayName,
        Long avatarImageId
) {
}

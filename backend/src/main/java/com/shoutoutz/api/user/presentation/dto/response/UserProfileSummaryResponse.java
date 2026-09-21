package com.shoutoutz.api.user.presentation.dto.response;

public record UserProfileSummaryResponse(
        String handle,
        String displayName,
        Long avatarImageId,
        String avatarUrl
) {
}

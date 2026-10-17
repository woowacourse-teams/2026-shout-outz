package com.shoutoutz.api.magazine.presentation.dto.response;

public record FeaturedUserResponse(
        String handle,
        String displayName,
        String bio,
        String avatarUrl
) {
}

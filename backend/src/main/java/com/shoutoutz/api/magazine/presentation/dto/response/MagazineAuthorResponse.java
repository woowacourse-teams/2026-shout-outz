package com.shoutoutz.api.magazine.presentation.dto.response;

public record MagazineAuthorResponse(
        String handle,
        String displayName,
        String avatarUrl
) {
}

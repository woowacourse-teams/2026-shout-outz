package com.shoutoutz.api.user.application.dto;

public record UserSearchCursor(
        int relevanceRank,
        String displayName,
        String handle
) {
}

package com.shoutoutz.api.user.application.query;

public record UserSearchCursor(
        int relevanceRank,
        String displayName,
        String handle
) {
}

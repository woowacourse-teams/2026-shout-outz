package com.shoutoutz.api.user.domain;

public record UserSearchCursor(
        int relevanceRank,
        String displayName,
        String handle
) {
}

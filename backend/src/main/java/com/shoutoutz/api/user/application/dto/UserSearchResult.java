package com.shoutoutz.api.user.application.dto;

import java.net.URI;
import java.util.List;
import java.util.Map;

public record UserSearchResult(
        List<UserSearchItem> items,
        String nextCursor,
        boolean hasNext,
        Map<Long, URI> avatarUrls
) {

    public UserSearchResult(List<UserSearchItem> items, String nextCursor, boolean hasNext) {
        this(items, nextCursor, hasNext, Map.of());
    }

    public UserSearchResult {
        items = items == null ? List.of() : List.copyOf(items);
        avatarUrls = avatarUrls == null ? Map.of() : Map.copyOf(avatarUrls);
    }
}

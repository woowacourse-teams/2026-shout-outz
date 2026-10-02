package com.shoutoutz.api.user.application.dto;

import java.net.URI;
import java.util.List;
import java.util.Map;

public record UserSearchResult(
        List<UserSearchItem> items,
        String nextCursor,
        boolean hasNext,
        long totalCount,
        Map<Long, URI> avatarUrls,
        Map<Long, String> userAvatarUrls
) {

    public UserSearchResult(
            List<UserSearchItem> items,
            String nextCursor,
            boolean hasNext,
            long totalCount,
            Map<Long, URI> avatarUrls
    ) {
        this(items, nextCursor, hasNext, totalCount, avatarUrls, Map.of());
    }

    public UserSearchResult {
        items = items == null ? List.of() : List.copyOf(items);
        avatarUrls = avatarUrls == null ? Map.of() : Map.copyOf(avatarUrls);
        userAvatarUrls = userAvatarUrls == null ? Map.of() : Map.copyOf(userAvatarUrls);
    }
}

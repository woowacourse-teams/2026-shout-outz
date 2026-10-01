package com.shoutoutz.api.feed.application.dto;

import java.net.URI;
import java.util.List;
import java.util.Map;

public record FeedFindAllResult(
        List<FeedItem> items,
        String nextCursor,
        boolean hasNext,
        long totalCount,
        Map<Long, URI> mediaUrls,
        Map<Long, String> userAvatarUrls
) {

    public FeedFindAllResult(
            List<FeedItem> items,
            String nextCursor,
            boolean hasNext,
            long totalCount,
            Map<Long, URI> mediaUrls
    ) {
        this(items, nextCursor, hasNext, totalCount, mediaUrls, Map.of());
    }

    public FeedFindAllResult {
        items = items == null ? List.of() : List.copyOf(items);
        mediaUrls = mediaUrls == null ? Map.of() : Map.copyOf(mediaUrls);
        userAvatarUrls = userAvatarUrls == null ? Map.of() : Map.copyOf(userAvatarUrls);
    }
}

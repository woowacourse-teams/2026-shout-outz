package com.shoutoutz.api.feed.application.dto;

import java.net.URI;
import java.util.List;
import java.util.Map;

public record FeedFindAllResult(
        List<FeedItem> items,
        String nextCursor,
        boolean hasNext,
        Map<Long, URI> mediaUrls
) {

    public FeedFindAllResult(List<FeedItem> items, String nextCursor, boolean hasNext) {
        this(items, nextCursor, hasNext, Map.of());
    }

    public FeedFindAllResult {
        items = items == null ? List.of() : List.copyOf(items);
        mediaUrls = mediaUrls == null ? Map.of() : Map.copyOf(mediaUrls);
    }
}

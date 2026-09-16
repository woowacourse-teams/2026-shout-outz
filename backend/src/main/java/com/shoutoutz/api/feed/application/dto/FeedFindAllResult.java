package com.shoutoutz.api.feed.application.dto;

import java.util.List;

public record FeedFindAllResult(
        List<FeedItem> items,
        String nextCursor,
        boolean hasNext
) {
}

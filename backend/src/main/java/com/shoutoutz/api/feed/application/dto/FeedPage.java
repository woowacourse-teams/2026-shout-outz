package com.shoutoutz.api.feed.application.dto;

import java.util.List;

public record FeedPage(
        List<FeedItem> items,
        boolean hasNext,
        long totalCount
) {

    public FeedPage {
        items = List.copyOf(items);
    }
}

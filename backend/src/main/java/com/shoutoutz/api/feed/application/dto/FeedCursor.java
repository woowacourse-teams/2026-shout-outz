package com.shoutoutz.api.feed.application.dto;

import java.time.Instant;

public record FeedCursor(
        FeedSort sort,
        int relevanceRank,
        long popularityScore,
        Instant createdAt,
        long feedId
) {
}

package com.shoutoutz.api.feed.application.dto;

import java.time.Instant;

public record FeedCursor(
        FeedSort sort,
        long likeCount,
        Instant createdAt,
        long feedId
) {
}

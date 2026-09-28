package com.shoutoutz.api.feed.domain;

public record FeedReactionCounts(
        long likeCount,
        long bookmarkCount
) {
}

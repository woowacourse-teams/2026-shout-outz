package com.shoutoutz.api.news.domain;

public record NewsReactionCounts(
        long likeCount,
        boolean likedByMe
) {
}

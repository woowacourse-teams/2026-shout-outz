package com.shoutoutz.api.feed.presentation.dto.response;

import com.shoutoutz.api.feed.domain.FeedReactionType;

public record FeedReactionResponse(
        long feedId,
        FeedReactionType type,
        boolean active,
        long likeCount,
        long bookmarkCount
) {
}

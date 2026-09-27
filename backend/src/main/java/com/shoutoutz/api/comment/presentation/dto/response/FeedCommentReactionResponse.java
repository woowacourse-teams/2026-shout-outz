package com.shoutoutz.api.comment.presentation.dto.response;

import com.shoutoutz.api.comment.domain.FeedCommentReactionType;

public record FeedCommentReactionResponse(
        long feedId,
        long commentId,
        FeedCommentReactionType type,
        boolean active,
        long agreeCount
) {
}

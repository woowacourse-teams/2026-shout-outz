package com.shoutoutz.api.comment.domain;

public record FeedCommentReactionCounts(
        long agreeCount,
        boolean agreedByMe
) {

    public FeedCommentReactionCounts(long agreeCount) {
        this(agreeCount, false);
    }
}

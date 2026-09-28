package com.shoutoutz.api.comment.domain;

public record ProjectCommentReactionCounts(
        long agreeCount,
        boolean agreedByMe
) {

    public ProjectCommentReactionCounts(long agreeCount) {
        this(agreeCount, false);
    }
}

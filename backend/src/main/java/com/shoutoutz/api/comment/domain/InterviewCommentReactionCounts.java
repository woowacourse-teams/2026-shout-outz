package com.shoutoutz.api.comment.domain;

public record InterviewCommentReactionCounts(
        long agreeCount,
        boolean agreedByMe
) {

    public InterviewCommentReactionCounts(long agreeCount) {
        this(agreeCount, false);
    }
}

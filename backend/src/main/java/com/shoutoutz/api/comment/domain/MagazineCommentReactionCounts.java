package com.shoutoutz.api.comment.domain;

public record MagazineCommentReactionCounts(
        long agreeCount,
        boolean agreedByMe
) {

    public MagazineCommentReactionCounts(long agreeCount) {
        this(agreeCount, false);
    }
}

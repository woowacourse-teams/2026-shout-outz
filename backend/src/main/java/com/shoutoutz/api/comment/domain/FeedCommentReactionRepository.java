package com.shoutoutz.api.comment.domain;

public interface FeedCommentReactionRepository {

    void add(long commentId, long userId, FeedCommentReactionType type);

    void remove(long commentId, long userId, FeedCommentReactionType type);

    FeedCommentReactionCounts countByCommentId(long commentId);
}

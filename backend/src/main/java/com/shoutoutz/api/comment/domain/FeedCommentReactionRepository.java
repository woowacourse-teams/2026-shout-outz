package com.shoutoutz.api.comment.domain;

import java.util.List;
import java.util.Map;

public interface FeedCommentReactionRepository {

    void add(long commentId, long userId, FeedCommentReactionType type);

    boolean remove(long commentId, long userId, FeedCommentReactionType type);

    FeedCommentReactionCounts countByCommentId(long commentId);

    Map<Long, FeedCommentReactionCounts> findByCommentIds(List<Long> commentIds, Long viewerId);
}

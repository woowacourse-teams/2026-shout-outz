package com.shoutoutz.api.comment.domain;

import java.util.List;
import java.util.Map;

public interface InterviewCommentReactionRepository {

    void add(long commentId, long userId, InterviewCommentReactionType type);

    boolean remove(long commentId, long userId, InterviewCommentReactionType type);

    InterviewCommentReactionCounts countByCommentId(long commentId);

    Map<Long, InterviewCommentReactionCounts> findByCommentIds(List<Long> commentIds, Long viewerId);
}

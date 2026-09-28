package com.shoutoutz.api.comment.domain;

import java.util.List;
import java.util.Map;

public interface ProjectCommentReactionRepository {

    void add(long commentId, long userId, ProjectCommentReactionType type);

    boolean remove(long commentId, long userId, ProjectCommentReactionType type);

    ProjectCommentReactionCounts countByCommentId(long commentId);

    Map<Long, ProjectCommentReactionCounts> findByCommentIds(List<Long> commentIds, Long viewerId);
}

package com.shoutoutz.api.comment.domain;

public interface ProjectCommentReactionRepository {

    void add(long commentId, long userId, ProjectCommentReactionType type);

    boolean remove(long commentId, long userId, ProjectCommentReactionType type);

    ProjectCommentReactionCounts countByCommentId(long commentId);
}

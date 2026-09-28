package com.shoutoutz.api.comment.application.dto;

import java.time.Instant;

/**
 * 피드와 프로젝트 댓글의 통합 조회 결과.
 */
public record UserCommentItem(
        long commentId,
        UserCommentType type,
        long targetId,
        String content,
        Instant createdAt,
        Instant updatedAt,
        long agreeCount,
        boolean agreedByMe
) {

    public UserCommentItem(
            long commentId,
            UserCommentType type,
            long targetId,
            String content,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(commentId, type, targetId, content, createdAt, updatedAt, 0L, false);
    }
}

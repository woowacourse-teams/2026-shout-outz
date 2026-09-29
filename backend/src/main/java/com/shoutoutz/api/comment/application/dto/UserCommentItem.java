package com.shoutoutz.api.comment.application.dto;

import java.time.Instant;

/**
 * 피드와 프로젝트 댓글의 통합 조회 결과.
 * 댓글이 달린 대상은 종류에 따라 feedId 나 projectSlug 중 하나만 채워지고, 나머지는 null 이다.
 */
public record UserCommentItem(
        long commentId,
        UserCommentType type,
        Long feedId,
        String projectSlug,
        String content,
        Instant createdAt,
        Instant updatedAt,
        long agreeCount,
        boolean agreedByMe
) {

    public UserCommentItem(
            long commentId,
            UserCommentType type,
            Long feedId,
            String projectSlug,
            String content,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(commentId, type, feedId, projectSlug, content, createdAt, updatedAt, 0L, false);
    }
}

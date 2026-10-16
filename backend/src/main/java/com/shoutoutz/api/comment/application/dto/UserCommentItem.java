package com.shoutoutz.api.comment.application.dto;

import java.time.Instant;

/**
 * 피드, 프로젝트, 인터뷰 댓글의 통합 조회 결과.
 * 댓글이 달린 대상은 종류에 따라 feedId, projectSlug, interviewId 중 하나만 채워지고, 나머지는 null 이다.
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
        boolean agreedByMe,
        Long interviewId
) {
    public UserCommentItem(long commentId, UserCommentType type, Long feedId, String projectSlug,
            String content, Instant createdAt, Instant updatedAt, long agreeCount, boolean agreedByMe) {
        this(commentId, type, feedId, projectSlug, content, createdAt, updatedAt, agreeCount, agreedByMe, null);
    }

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

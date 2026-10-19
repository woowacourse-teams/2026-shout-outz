package com.shoutoutz.api.comment.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shoutoutz.api.comment.application.dto.UserCommentItem;
import com.shoutoutz.api.comment.application.dto.UserCommentType;
import java.time.Instant;
import java.util.List;

public record UserCommentResponse(
        long commentId,
        UserCommentType type,
        Long feedId,
        String projectSlug,
        String content,
        Instant createdAt,
        Instant updatedAt,
        long agreeCount,
        boolean agreedByMe,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long magazineId
) {

    public UserCommentResponse(long commentId, UserCommentType type, Long feedId, String projectSlug,
            String content, Instant createdAt, Instant updatedAt, long agreeCount, boolean agreedByMe) {
        this(commentId, type, feedId, projectSlug, content, createdAt, updatedAt, agreeCount, agreedByMe, null);
    }

    public static List<UserCommentResponse> from(List<UserCommentItem> comments) {
        return comments.stream()
                .map(UserCommentResponse::from)
                .toList();
    }

    private static UserCommentResponse from(UserCommentItem comment) {
        return new UserCommentResponse(
                comment.commentId(),
                comment.type(),
                comment.feedId(),
                comment.projectSlug(),
                comment.content(),
                comment.createdAt(),
                comment.updatedAt(),
                comment.agreeCount(),
                comment.agreedByMe(),
                comment.magazineId()
        );
    }
}

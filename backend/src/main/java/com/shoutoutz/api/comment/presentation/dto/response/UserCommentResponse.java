package com.shoutoutz.api.comment.presentation.dto.response;

import com.shoutoutz.api.comment.application.dto.UserCommentItem;
import com.shoutoutz.api.comment.application.dto.UserCommentType;
import java.time.Instant;
import java.util.List;

public record UserCommentResponse(
        long commentId,
        UserCommentType type,
        long targetId,
        String content,
        Instant createdAt,
        Instant updatedAt
) {

    public static List<UserCommentResponse> from(List<UserCommentItem> comments) {
        return comments.stream()
                .map(UserCommentResponse::from)
                .toList();
    }

    private static UserCommentResponse from(UserCommentItem comment) {
        return new UserCommentResponse(
                comment.commentId(),
                comment.type(),
                comment.targetId(),
                comment.content(),
                comment.createdAt(),
                comment.updatedAt()
        );
    }
}

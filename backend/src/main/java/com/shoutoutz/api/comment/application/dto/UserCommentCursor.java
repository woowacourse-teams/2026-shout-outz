package com.shoutoutz.api.comment.application.dto;

import java.time.Instant;

public record UserCommentCursor(
        Instant createdAt,
        UserCommentType type,
        long commentId
) {
}

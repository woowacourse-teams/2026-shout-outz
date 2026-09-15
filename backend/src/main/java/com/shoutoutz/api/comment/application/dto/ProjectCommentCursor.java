package com.shoutoutz.api.comment.application.dto;

import com.shoutoutz.api.comment.domain.ProjectCommentSort;
import java.time.Instant;

public record ProjectCommentCursor(
        Instant createdAt,
        long id,
        ProjectCommentSort sort
) {
}

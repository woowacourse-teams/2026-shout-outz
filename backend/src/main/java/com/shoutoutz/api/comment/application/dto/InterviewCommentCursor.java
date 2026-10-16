package com.shoutoutz.api.comment.application.dto;

import com.shoutoutz.api.comment.domain.InterviewCommentSort;
import java.time.Instant;

public record InterviewCommentCursor(
        Instant createdAt,
        long id,
        InterviewCommentSort sort
) {
}

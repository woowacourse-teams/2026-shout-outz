package com.shoutoutz.api.comment.application.dto;

import com.shoutoutz.api.comment.domain.FeedCommentSort;
import java.time.Instant;

public record FeedCommentCursor(
        Instant createdAt,
        long id,
        FeedCommentSort sort
) {
}

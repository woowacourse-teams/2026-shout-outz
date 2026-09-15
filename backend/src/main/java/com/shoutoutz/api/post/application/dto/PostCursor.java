package com.shoutoutz.api.post.application.dto;

import java.time.Instant;

public record PostCursor(
        PostSort sort,
        long likeCount,
        Instant createdAt,
        long postId
) {
}

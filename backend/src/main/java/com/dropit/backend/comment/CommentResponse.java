package com.dropit.backend.comment;

import java.time.Instant;
import java.util.UUID;

import com.dropit.backend.maker.MakerResponse;

public record CommentResponse(
    UUID id,
    String appId,
    UUID userId,
    UUID parentId,
    String title,
    String content,
    Instant createdAt,
    MakerResponse author
) {
}

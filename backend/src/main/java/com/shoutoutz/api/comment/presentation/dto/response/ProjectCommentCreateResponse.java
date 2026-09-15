package com.shoutoutz.api.comment.presentation.dto.response;

import java.time.Instant;

public record ProjectCommentCreateResponse(
        Long id,
        String content,
        Author author,
        Long parentId,
        Instant createdAt,
        Instant updatedAt,
        boolean editable
) {

    public record Author(
            Long userId,
            String displayName,
            Long avatarImageId
    ) {
    }
}

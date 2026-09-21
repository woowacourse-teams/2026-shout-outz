package com.shoutoutz.api.comment.presentation.dto.response;

import java.time.Instant;

public record ProjectCommentUpdateResponse(
        Long id,
        String content,
        Author author,
        Long parentId,
        Instant createdAt,
        Instant updatedAt,
        boolean editable,
        boolean edited
) {

    public record Author(
            Long userId,
            String displayName,
            String avatarUrl
    ) {
    }
}

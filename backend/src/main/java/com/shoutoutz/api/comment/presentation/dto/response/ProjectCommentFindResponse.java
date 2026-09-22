package com.shoutoutz.api.comment.presentation.dto.response;

import java.time.Instant;
import java.util.List;

public record ProjectCommentFindResponse(
        List<Comment> comments,
        Meta meta
) {

    public ProjectCommentFindResponse {
        comments = List.copyOf(comments);
    }

    public record Comment(
            Long id,
            String content,
            Author author,
            Long parentId,
            Instant createdAt,
            Instant updatedAt,
            boolean editable,
            boolean edited,
            boolean deleted
    ) {
    }

    public record Author(
            Long userId,
            String displayName,
            Long avatarImageId,
            String avatarUrl
    ) {
    }

    public record Meta(
            String nextCursor,
            boolean hasNext
    ) {
    }
}

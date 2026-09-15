package com.shoutoutz.api.comment.presentation.dto.response;

import java.time.Instant;
import java.util.List;

public record FeedCommentFindResponse(
        List<Comment> comments,
        Meta meta
) {

    public FeedCommentFindResponse {
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
            Long avatarImageId
    ) {
    }

    public record Meta(
            String nextCursor,
            boolean hasNext
    ) {
    }
}

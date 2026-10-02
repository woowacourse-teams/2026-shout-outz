package com.shoutoutz.api.comment.application.dto;

import java.util.List;

public record UserCommentResult(
        List<UserCommentItem> comments,
        String nextCursor,
        boolean hasNext,
        long totalCount
) {

    public UserCommentResult {
        comments = List.copyOf(comments);
    }
}

package com.shoutoutz.api.comment.presentation.dto.response;

import com.shoutoutz.api.comment.domain.ProjectCommentReactionType;

public record ProjectCommentReactionResponse(
        String slug,
        long commentId,
        ProjectCommentReactionType type,
        boolean active,
        long agreeCount
) {
}

package com.shoutoutz.api.comment.presentation.dto.response;

import com.shoutoutz.api.comment.domain.MagazineCommentReactionType;

public record MagazineCommentReactionResponse(
        long magazineId,
        long commentId,
        MagazineCommentReactionType type,
        boolean active,
        long agreeCount
) {
}

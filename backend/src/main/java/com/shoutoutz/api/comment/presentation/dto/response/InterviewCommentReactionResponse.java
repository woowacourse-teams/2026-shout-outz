package com.shoutoutz.api.comment.presentation.dto.response;

import com.shoutoutz.api.comment.domain.InterviewCommentReactionType;

public record InterviewCommentReactionResponse(
        long interviewId,
        long commentId,
        InterviewCommentReactionType type,
        boolean active,
        long agreeCount
) {
}

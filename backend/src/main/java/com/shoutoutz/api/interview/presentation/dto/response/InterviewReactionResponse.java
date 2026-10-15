package com.shoutoutz.api.interview.presentation.dto.response;

import com.shoutoutz.api.interview.domain.InterviewReactionType;

public record InterviewReactionResponse(
        long interviewId,
        InterviewReactionType type,
        boolean active,
        long likeCount
) {
}

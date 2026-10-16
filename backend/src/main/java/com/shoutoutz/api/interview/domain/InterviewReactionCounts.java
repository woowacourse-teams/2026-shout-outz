package com.shoutoutz.api.interview.domain;

public record InterviewReactionCounts(
        long likeCount,
        boolean likedByMe
) {
}

package com.shoutoutz.api.interview.presentation.dto.response;

import com.shoutoutz.api.interview.application.dto.InterviewItem;
import com.shoutoutz.api.interview.domain.InterviewReactionCounts;
import com.shoutoutz.api.interview.domain.InterviewStatus;
import java.net.URI;
import java.time.Instant;

public record InterviewSummaryResponse(
        long id,
        String title,
        String summary,
        URI thumbnailUrl,
        IntervieweeResponse interviewee,
        InterviewStatus status,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        long likeCount,
        boolean likedByMe,
        long commentCount,
        long viewCount
) {
    public static InterviewSummaryResponse from(
            InterviewItem item,
            URI thumbnailUrl,
            IntervieweeResponse interviewee,
            InterviewReactionCounts reactions
    ) {
        return new InterviewSummaryResponse(
                item.id(), item.title(), item.summary(), thumbnailUrl, interviewee, item.status(),
                item.publishedAt(), item.createdAt(), item.updatedAt(),
                reactions.likeCount(), reactions.likedByMe(), item.commentCount(), item.viewCount()
        );
    }
}

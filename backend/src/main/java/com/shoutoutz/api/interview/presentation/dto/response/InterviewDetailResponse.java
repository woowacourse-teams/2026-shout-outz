package com.shoutoutz.api.interview.presentation.dto.response;

import com.shoutoutz.api.interview.application.dto.InterviewItem;
import com.shoutoutz.api.interview.domain.InterviewReactionCounts;
import java.net.URI;
import java.time.Instant;

public record InterviewDetailResponse(
        long id,
        String title,
        String summary,
        URI thumbnailUrl,
        IntervieweeResponse interviewee,
        String contentMd,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        long likeCount,
        boolean likedByMe,
        long commentCount,
        long viewCount
) {
    public static InterviewDetailResponse from(
            InterviewItem item,
            URI thumbnailUrl,
            IntervieweeResponse interviewee,
            String contentMd,
            InterviewReactionCounts reactions,
            long viewCount
    ) {
        return new InterviewDetailResponse(
                item.id(), item.title(), item.summary(), thumbnailUrl, interviewee, contentMd,
                item.publishedAt(), item.createdAt(), item.updatedAt(),
                reactions.likeCount(), reactions.likedByMe(), item.commentCount(), viewCount
        );
    }
}

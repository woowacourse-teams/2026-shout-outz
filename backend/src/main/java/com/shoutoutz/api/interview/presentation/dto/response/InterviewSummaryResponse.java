package com.shoutoutz.api.interview.presentation.dto.response;

import com.shoutoutz.api.interview.domain.InterviewStatus;
import java.net.URI;
import java.time.Instant;

public record InterviewSummaryResponse(
        long id, String title, String summary, URI thumbnailUrl, IntervieweeResponse interviewee,
        InterviewStatus status, Instant publishedAt, Instant createdAt, Instant updatedAt, long likeCount, boolean likedByMe, long commentCount, long viewCount
) {
}

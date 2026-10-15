package com.shoutoutz.api.interview.presentation.dto.response;

import java.net.URI;
import java.time.Instant;

public record InterviewDetailResponse(
        long id, String title, String summary, URI thumbnailUrl, IntervieweeResponse interviewee,
        String contentMd, Instant publishedAt, Instant createdAt, Instant updatedAt
) {
}

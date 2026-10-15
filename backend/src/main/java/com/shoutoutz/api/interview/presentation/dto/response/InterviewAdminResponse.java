package com.shoutoutz.api.interview.presentation.dto.response;

import com.shoutoutz.api.interview.domain.Interview;
import com.shoutoutz.api.interview.domain.InterviewStatus;
import java.time.Instant;

public record InterviewAdminResponse(
        long id, String title, String summary, long intervieweeId, Long thumbnailMediaId,
        String contentMd, InterviewStatus status, Instant publishedAt, Instant createdAt, Instant updatedAt
) {
    public static InterviewAdminResponse from(Interview interview) {
        return new InterviewAdminResponse(interview.getId(), interview.getTitle(), interview.getSummary(),
                interview.getIntervieweeId(), interview.getThumbnailMediaId(), interview.getContentMd(),
                interview.getStatus(), interview.getPublishedAt(), interview.getCreatedAt(), interview.getUpdatedAt());
    }
}

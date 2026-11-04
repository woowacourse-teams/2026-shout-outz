package com.shoutoutz.api.jobposting.presentation.dto.response;

import com.shoutoutz.api.jobposting.domain.JobPosting;
import com.shoutoutz.api.jobposting.domain.MatchingStatus;
import java.time.Instant;

public record AdminJobPostingSaveResponse(
        long jobPostingId,
        MatchingStatus status,
        Instant createdAt
) {

    public static AdminJobPostingSaveResponse from(JobPosting jobPosting) {
        return new AdminJobPostingSaveResponse(
                jobPosting.getId(),
                jobPosting.getStatus(),
                jobPosting.getCreatedAt()
        );
    }
}

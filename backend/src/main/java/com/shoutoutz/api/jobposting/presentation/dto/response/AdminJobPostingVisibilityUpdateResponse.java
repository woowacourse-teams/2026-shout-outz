package com.shoutoutz.api.jobposting.presentation.dto.response;

import com.shoutoutz.api.jobposting.domain.JobPostingVisibility;
import java.time.Instant;

public record AdminJobPostingVisibilityUpdateResponse(
        long jobPostingId,
        boolean hidden,
        Instant hiddenAt
) {

    public static AdminJobPostingVisibilityUpdateResponse from(JobPostingVisibility visibility) {
        return new AdminJobPostingVisibilityUpdateResponse(
                visibility.jobPostingId(),
                visibility.hidden(),
                visibility.hiddenAt()
        );
    }
}

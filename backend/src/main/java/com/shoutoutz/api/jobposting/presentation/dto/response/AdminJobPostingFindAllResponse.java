package com.shoutoutz.api.jobposting.presentation.dto.response;

import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.jobposting.application.dto.JobPostingAdminSummary;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.MatchingStatus;
import java.time.Instant;
import java.util.List;

public record AdminJobPostingFindAllResponse(
        List<Item> items,
        SliceMetaResponse meta
) {

    public AdminJobPostingFindAllResponse {
        items = List.copyOf(items);
    }

    public static AdminJobPostingFindAllResponse from(List<JobPostingAdminSummary> summaries, SliceMetaResponse meta) {
        return new AdminJobPostingFindAllResponse(
                summaries.stream().map(Item::from).toList(),
                meta
        );
    }

    public record Item(
            long jobPostingId,
            String companyName,
            String title,
            JobRole jobRole,
            MatchingStatus status,
            int failedRequirementCount,
            Instant hiddenAt,
            Instant createdAt
    ) {

        private static Item from(JobPostingAdminSummary summary) {
            return new Item(
                    summary.id(),
                    summary.companyName(),
                    summary.title(),
                    summary.jobRole(),
                    summary.status(),
                    summary.failedRequirementCount(),
                    summary.hiddenAt(),
                    summary.createdAt()
            );
        }
    }
}

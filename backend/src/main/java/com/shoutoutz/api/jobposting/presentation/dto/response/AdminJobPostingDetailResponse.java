package com.shoutoutz.api.jobposting.presentation.dto.response;

import com.shoutoutz.api.jobposting.domain.JobPosting;
import com.shoutoutz.api.jobposting.domain.JobRequirement;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.MatchingStatus;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AdminJobPostingDetailResponse(
        long jobPostingId,
        String sourceUrl,
        String companyName,
        JobRole jobRole,
        String careerRequirement,
        String title,
        String employmentType,
        LocalDate deadline,
        MatchingStatus status,
        long registeredBy,
        Instant createdAt,
        List<Requirement> requirements
) {

    public AdminJobPostingDetailResponse {
        requirements = List.copyOf(requirements);
    }

    public static AdminJobPostingDetailResponse from(JobPosting jobPosting) {
        return new AdminJobPostingDetailResponse(
                jobPosting.getId(),
                jobPosting.getSourceUrl().value(),
                jobPosting.getCompanyName(),
                jobPosting.getJobRole(),
                jobPosting.getCareerRequirement(),
                jobPosting.getTitle(),
                jobPosting.getEmploymentType(),
                jobPosting.getDeadline(),
                jobPosting.getStatus(),
                jobPosting.getRegisteredBy(),
                jobPosting.getCreatedAt(),
                jobPosting.getRequirements().stream().map(Requirement::from).toList()
        );
    }

    public record Requirement(
            long requirementId,
            int displayOrder,
            String name,
            RequirementCategory category,
            String summary,
            MatchingStatus status
    ) {

        private static Requirement from(JobRequirement requirement) {
            return new Requirement(
                    requirement.getId(),
                    requirement.getDisplayOrder(),
                    requirement.getName(),
                    requirement.getCategory(),
                    requirement.getSummary(),
                    requirement.getStatus()
            );
        }
    }
}

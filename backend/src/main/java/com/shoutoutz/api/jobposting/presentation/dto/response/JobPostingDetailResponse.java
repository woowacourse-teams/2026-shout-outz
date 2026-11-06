package com.shoutoutz.api.jobposting.presentation.dto.response;

import com.shoutoutz.api.jobposting.domain.JobPosting;
import com.shoutoutz.api.jobposting.domain.JobRequirement;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 사용자 채용공고 상세. 마감일이 없으면 상시 채용이라 deadline 은 null 이다.
 */
public record JobPostingDetailResponse(
        long jobPostingId,
        String companyName,
        JobRole jobRole,
        String employmentType,
        String careerRequirement,
        String title,
        LocalDate deadline,
        String sourceUrl,
        List<Requirement> requirements
) {

    public JobPostingDetailResponse {
        requirements = List.copyOf(requirements);
    }

    /**
     * 역량은 공고에 적힌 순서대로 담고, 추천 자료가 없는 역량의 자료 수는 0이다.
     */
    public static JobPostingDetailResponse from(JobPosting jobPosting, Map<Long, Long> recommendationCounts) {
        return new JobPostingDetailResponse(
                jobPosting.getId(),
                jobPosting.getCompanyName(),
                jobPosting.getJobRole(),
                jobPosting.getEmploymentType(),
                jobPosting.getCareerRequirement(),
                jobPosting.getTitle(),
                jobPosting.getDeadline(),
                jobPosting.getSourceUrl().value(),
                jobPosting.getRequirements().stream()
                        .map(requirement -> Requirement.from(
                                requirement,
                                recommendationCounts.getOrDefault(requirement.getId(), 0L)))
                        .toList()
        );
    }

    public record Requirement(
            long requirementId,
            int displayOrder,
            String name,
            RequirementCategory category,
            String summary,
            long recommendationCount
    ) {

        private static Requirement from(JobRequirement requirement, long recommendationCount) {
            return new Requirement(
                    requirement.getId(),
                    requirement.getDisplayOrder(),
                    requirement.getName(),
                    requirement.getCategory(),
                    requirement.getSummary(),
                    recommendationCount
            );
        }
    }
}

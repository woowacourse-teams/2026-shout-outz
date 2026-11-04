package com.shoutoutz.api.jobposting.presentation.dto.response;

import com.shoutoutz.api.jobposting.application.dto.CandidateChunk;
import com.shoutoutz.api.jobposting.application.dto.CandidateChunk.ChunkKind;
import com.shoutoutz.api.jobposting.application.dto.RequirementRecommendationDetail;
import com.shoutoutz.api.jobposting.domain.JobPosting;
import com.shoutoutz.api.jobposting.domain.JobRequirement;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.MatchingFailure;
import com.shoutoutz.api.jobposting.domain.MatchingStatus;
import com.shoutoutz.api.jobposting.domain.RecommendationVerdict;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        Instant hiddenAt,
        long registeredBy,
        Instant createdAt,
        List<Requirement> requirements
) {

    public AdminJobPostingDetailResponse {
        requirements = List.copyOf(requirements);
    }

    public static AdminJobPostingDetailResponse from(
            JobPosting jobPosting,
            Map<Long, MatchingFailure> lastErrors,
            List<RequirementRecommendationDetail> recommendations
    ) {
        Map<Long, List<RequirementRecommendationDetail>> recommendationsByRequirement = recommendations.stream()
                .collect(Collectors.groupingBy(RequirementRecommendationDetail::requirementId));
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
                jobPosting.getHiddenAt(),
                jobPosting.getRegisteredBy(),
                jobPosting.getCreatedAt(),
                jobPosting.getRequirements().stream()
                        .map(requirement -> Requirement.from(
                                requirement,
                                lastErrors.get(requirement.getId()),
                                recommendationsByRequirement.getOrDefault(requirement.getId(), List.of())
                        ))
                        .toList()
        );
    }

    public record Requirement(
            long requirementId,
            int displayOrder,
            String name,
            RequirementCategory category,
            String summary,
            MatchingStatus status,
            MatchingFailure lastError,
            List<Recommendation> recommendations
    ) {

        public Requirement {
            recommendations = List.copyOf(recommendations);
        }

        private static Requirement from(
                JobRequirement requirement,
                MatchingFailure lastError,
                List<RequirementRecommendationDetail> recommendations
        ) {
            return new Requirement(
                    requirement.getId(),
                    requirement.getDisplayOrder(),
                    requirement.getName(),
                    requirement.getCategory(),
                    requirement.getSummary(),
                    requirement.getStatus(),
                    lastError,
                    recommendations.stream().map(Recommendation::from).toList()
            );
        }
    }

    public record Recommendation(
            String source,
            String externalKey,
            String title,
            String url,
            RecommendationVerdict verdict,
            String reason,
            String description,
            double similarityScore,
            List<Evidence> evidence
    ) {

        public Recommendation {
            evidence = List.copyOf(evidence);
        }

        private static Recommendation from(RequirementRecommendationDetail recommendation) {
            return new Recommendation(
                    recommendation.source(),
                    recommendation.externalKey(),
                    recommendation.title(),
                    recommendation.url(),
                    recommendation.verdict(),
                    recommendation.reason(),
                    recommendation.description(),
                    recommendation.similarityScore(),
                    recommendation.evidence().stream().map(Evidence::from).toList()
            );
        }
    }

    public record Evidence(ChunkKind kind, String text, double similarityScore) {

        private static Evidence from(CandidateChunk chunk) {
            return new Evidence(chunk.kind(), chunk.text(), chunk.similarityScore());
        }
    }
}

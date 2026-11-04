package com.shoutoutz.api.jobposting.infrastructure.mapper;

import com.shoutoutz.api.jobposting.domain.JobPosting;
import com.shoutoutz.api.jobposting.domain.JobRequirement;
import com.shoutoutz.api.jobposting.infrastructure.JobPostingEntity;
import com.shoutoutz.api.jobposting.infrastructure.JobRequirementEntity;
import java.util.List;

public final class JobPostingMapper {

    private static final int INITIAL_ATTEMPT_COUNT = 0;

    private JobPostingMapper() {
    }

    public static JobPostingEntity toEntity(JobPosting jobPosting) {
        return JobPostingEntity.builder()
                .id(jobPosting.getId())
                .sourceUrl(jobPosting.getSourceUrl().value())
                .contentHash(jobPosting.getContentHash().value())
                .companyName(jobPosting.getCompanyName())
                .jobRole(jobPosting.getJobRole())
                .careerRequirement(jobPosting.getCareerRequirement())
                .title(jobPosting.getTitle())
                .employmentType(jobPosting.getEmploymentType())
                .deadline(jobPosting.getDeadline())
                .status(jobPosting.getStatus())
                .hiddenAt(jobPosting.getHiddenAt())
                .registeredBy(jobPosting.getRegisteredBy())
                .createdAt(jobPosting.getCreatedAt())
                .updatedAt(jobPosting.getUpdatedAt())
                .build();
    }

    /**
     * 역량 항목은 공고를 등록하거나 원문을 바꾼 시점에 만들어지므로 생성·수정 시각 모두 공고의 수정 시각을 쓴다.
     * 공고를 고칠 때 공고의 생성 시각을 쓰면 새로 넣은 역량이 처음 등록한 때 만든 것처럼 남는다.
     * 매칭 작업용 칸은 처음 값으로 둔다.
     */
    public static List<JobRequirementEntity> toRequirementEntities(long jobPostingId, JobPosting jobPosting) {
        return jobPosting.getRequirements().stream()
                .map(requirement -> JobRequirementEntity.builder()
                        .id(requirement.getId())
                        .jobPostingId(jobPostingId)
                        .displayOrder(requirement.getDisplayOrder())
                        .name(requirement.getName())
                        .category(requirement.getCategory())
                        .summary(requirement.getSummary())
                        .status(requirement.getStatus())
                        .attemptCount(INITIAL_ATTEMPT_COUNT)
                        .createdAt(jobPosting.getUpdatedAt())
                        .updatedAt(jobPosting.getUpdatedAt())
                        .build())
                .toList();
    }

    public static JobPosting toDomain(JobPostingEntity entity, List<JobRequirementEntity> requirementEntities) {
        return JobPosting.reconstitute(
                entity.getId(),
                entity.getSourceUrl(),
                entity.getContentHash(),
                entity.getCompanyName(),
                entity.getJobRole(),
                entity.getCareerRequirement(),
                entity.getTitle(),
                entity.getEmploymentType(),
                entity.getDeadline(),
                entity.getStatus(),
                entity.getHiddenAt(),
                entity.getRegisteredBy(),
                requirementEntities.stream()
                        .map(JobPostingMapper::toRequirementDomain)
                        .toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static JobRequirement toRequirementDomain(JobRequirementEntity entity) {
        return JobRequirement.reconstitute(
                entity.getId(),
                entity.getDisplayOrder(),
                entity.getName(),
                entity.getCategory(),
                entity.getSummary(),
                entity.getStatus()
        );
    }
}

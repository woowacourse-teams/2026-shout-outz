package com.shoutoutz.api.jobposting.domain;

import com.shoutoutz.api.common.util.DataResolveUtil;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;

/**
 * 운영자가 등록한 채용공고와 공고에서 뽑은 역량 항목 목록의 aggregate root.
 *
 * 공고 원문은 저작권 위험 때문에 갖지 않고, 같은 내용인지 판별할 {@link JobPostingContentHash}만 갖는다.
 */
@Getter
public final class JobPosting {

    private final Long id;
    private final JobPostingUrl sourceUrl;
    private final JobPostingContentHash contentHash;
    private final String companyName;
    private final JobRole jobRole;
    private final String careerRequirement;
    private final String title;
    private final String employmentType;
    private final LocalDate deadline;
    private final MatchingStatus status;
    private final Instant hiddenAt;
    private final Long registeredBy;
    private final List<JobRequirement> requirements;
    private final Instant createdAt;
    private final Instant updatedAt;

    private JobPosting(
            Long id,
            JobPostingUrl sourceUrl,
            JobPostingContentHash contentHash,
            String companyName,
            JobRole jobRole,
            String careerRequirement,
            String title,
            String employmentType,
            LocalDate deadline,
            MatchingStatus status,
            Instant hiddenAt,
            Long registeredBy,
            List<JobRequirement> requirements,
            Instant createdAt,
            Instant updatedAt
    ) {
        String sanitizedCompanyName = DataResolveUtil.sanitizeString(companyName);
        String sanitizedCareerRequirement = DataResolveUtil.sanitizeString(careerRequirement);
        String sanitizedTitle = DataResolveUtil.sanitizeString(title);
        String sanitizedEmploymentType = DataResolveUtil.sanitizeString(employmentType);

        JobPostingValidator.validateJobPosting(
                id,
                sourceUrl,
                contentHash,
                sanitizedCompanyName,
                jobRole,
                sanitizedCareerRequirement,
                sanitizedTitle,
                sanitizedEmploymentType,
                status,
                registeredBy,
                requirements,
                createdAt,
                updatedAt
        );

        this.id = id;
        this.sourceUrl = sourceUrl;
        this.contentHash = contentHash;
        this.companyName = sanitizedCompanyName;
        this.jobRole = jobRole;
        this.careerRequirement = sanitizedCareerRequirement;
        this.title = sanitizedTitle;
        this.employmentType = sanitizedEmploymentType;
        this.deadline = deadline;
        this.status = status;
        this.hiddenAt = hiddenAt;
        this.registeredBy = registeredBy;
        this.requirements = List.copyOf(requirements);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 운영자가 확인한 공고 정보와 역량 목록으로 새 공고를 만든다.
     * 원문은 해시만 만들고 갖지 않는다. 매칭 전이므로 상태는 PENDING 이다.
     */
    public static JobPosting register(
            String sourceUrl,
            String content,
            String companyName,
            JobRole jobRole,
            String careerRequirement,
            String title,
            String employmentType,
            LocalDate deadline,
            long registeredBy,
            List<JobRequirement> requirements,
            Instant now
    ) {
        return new JobPosting(
                null,
                new JobPostingUrl(sourceUrl),
                JobPostingContentHash.from(content),
                companyName,
                jobRole,
                careerRequirement,
                title,
                employmentType,
                deadline,
                MatchingStatus.PENDING,
                null,
                registeredBy,
                requirements,
                now,
                now
        );
    }

    public static JobPosting reconstitute(
            long id,
            String sourceUrl,
            String contentHash,
            String companyName,
            JobRole jobRole,
            String careerRequirement,
            String title,
            String employmentType,
            LocalDate deadline,
            MatchingStatus status,
            Instant hiddenAt,
            long registeredBy,
            List<JobRequirement> requirements,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new JobPosting(
                id,
                new JobPostingUrl(sourceUrl),
                new JobPostingContentHash(contentHash),
                companyName,
                jobRole,
                careerRequirement,
                title,
                employmentType,
                deadline,
                status,
                hiddenAt,
                registeredBy,
                requirements,
                createdAt,
                updatedAt
        );
    }
}

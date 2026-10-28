package com.shoutoutz.api.jobposting.domain;

import static com.shoutoutz.api.common.validator.DomainValidator.validateIntRange;
import static com.shoutoutz.api.common.validator.DomainValidator.validateLongMinSize;
import static com.shoutoutz.api.common.validator.DomainValidator.validateMaxLength;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNull;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNullOrBlank;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_COMPANY_NAME_NULL_OR_BLANK;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_CREATED_AT_NULL;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_INVALID_CAREER_REQUIREMENT;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_INVALID_COMPANY_NAME_LENGTH;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_INVALID_CONTENT_HASH;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_INVALID_EMPLOYMENT_TYPE;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_INVALID_ID_SIZE;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_INVALID_REGISTERED_BY_SIZE;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_INVALID_REQUIREMENT_COUNT;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_INVALID_REQUIREMENT_ORDER;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_INVALID_TITLE_LENGTH;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_INVALID_URL;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_JOB_ROLE_NULL;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_REGISTERED_BY_NULL;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_STATUS_NULL;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_TITLE_NULL_OR_BLANK;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_UPDATED_AT_NULL;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_REQUIREMENT_CATEGORY_NULL;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_REQUIREMENT_INVALID_ID_SIZE;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_REQUIREMENT_INVALID_NAME_LENGTH;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_REQUIREMENT_INVALID_SUMMARY_LENGTH;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_REQUIREMENT_NAME_NULL_OR_BLANK;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_REQUIREMENT_STATUS_NULL;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_REQUIREMENT_SUMMARY_NULL_OR_BLANK;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.time.Instant;
import java.util.List;

/**
 * 채용공고 도메인 전용 Validator.
 * 길이 제한은 job_postings, job_requirements 테이블의 칸 길이와 같다.
 */
final class JobPostingValidator {

    private static final int MIN_ID_SIZE = 1;
    private static final int MIN_REGISTERED_BY_SIZE = 1;
    private static final int MAX_COMPANY_NAME_LENGTH = 100;
    private static final int MAX_CAREER_REQUIREMENT_LENGTH = 50;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_EMPLOYMENT_TYPE_LENGTH = 30;
    private static final int MIN_REQUIREMENT_COUNT = 1;
    private static final int MAX_REQUIREMENT_COUNT = 50;
    private static final int MIN_DISPLAY_ORDER = 1;
    private static final int MAX_REQUIREMENT_NAME_LENGTH = 100;
    private static final int MAX_REQUIREMENT_SUMMARY_LENGTH = 500;

    private JobPostingValidator() {
    }

    static void validateJobPosting(
            Long id,
            JobPostingUrl sourceUrl,
            JobPostingContentHash contentHash,
            String companyName,
            JobRole jobRole,
            String careerRequirement,
            String title,
            String employmentType,
            MatchingStatus status,
            Long registeredBy,
            List<JobRequirement> requirements,
            Instant createdAt,
            Instant updatedAt
    ) {
        validateId(id, JOB_POSTING_INVALID_ID_SIZE);
        validateNotNull(sourceUrl, JOB_POSTING_INVALID_URL);
        validateNotNull(contentHash, JOB_POSTING_INVALID_CONTENT_HASH);
        validateCompanyName(companyName);
        validateNotNull(jobRole, JOB_POSTING_JOB_ROLE_NULL);
        validateCareerRequirement(careerRequirement);
        validateTitle(title);
        validateEmploymentType(employmentType);
        validateNotNull(status, JOB_POSTING_STATUS_NULL);
        validateRegisteredBy(registeredBy);
        validateRequirements(requirements);
        validateNotNull(createdAt, JOB_POSTING_CREATED_AT_NULL);
        validateNotNull(updatedAt, JOB_POSTING_UPDATED_AT_NULL);
    }

    static void validateJobRequirement(
            Long id,
            int displayOrder,
            String name,
            RequirementCategory category,
            String summary,
            MatchingStatus status
    ) {
        validateId(id, JOB_REQUIREMENT_INVALID_ID_SIZE);
        validateIntRange(displayOrder, MIN_DISPLAY_ORDER, MAX_REQUIREMENT_COUNT, JOB_POSTING_INVALID_REQUIREMENT_ORDER);
        validateRequirementName(name);
        validateNotNull(category, JOB_REQUIREMENT_CATEGORY_NULL);
        validateRequirementSummary(summary);
        validateNotNull(status, JOB_REQUIREMENT_STATUS_NULL);
    }

    private static void validateId(Long id, JobPostingErrorCode errorCode) {
        if (id == null) {
            return;
        }
        validateLongMinSize(id, MIN_ID_SIZE, errorCode);
    }

    private static void validateCompanyName(String companyName) {
        validateNotNullOrBlank(companyName, JOB_POSTING_COMPANY_NAME_NULL_OR_BLANK);
        validateMaxLength(companyName, MAX_COMPANY_NAME_LENGTH, JOB_POSTING_INVALID_COMPANY_NAME_LENGTH);
    }

    /**
     * 공고에 적혀 있지 않을 수 있어 null 을 허용한다. 공백뿐인 값은 정제 단계에서 null 이 된다.
     */
    private static void validateCareerRequirement(String careerRequirement) {
        if (careerRequirement == null) {
            return;
        }
        validateMaxLength(careerRequirement, MAX_CAREER_REQUIREMENT_LENGTH, JOB_POSTING_INVALID_CAREER_REQUIREMENT);
    }

    private static void validateTitle(String title) {
        validateNotNullOrBlank(title, JOB_POSTING_TITLE_NULL_OR_BLANK);
        validateMaxLength(title, MAX_TITLE_LENGTH, JOB_POSTING_INVALID_TITLE_LENGTH);
    }

    /**
     * 공고에 적혀 있지 않을 수 있어 null 을 허용한다. 공백뿐인 값은 정제 단계에서 null 이 된다.
     */
    private static void validateEmploymentType(String employmentType) {
        if (employmentType == null) {
            return;
        }
        validateMaxLength(employmentType, MAX_EMPLOYMENT_TYPE_LENGTH, JOB_POSTING_INVALID_EMPLOYMENT_TYPE);
    }

    private static void validateRegisteredBy(Long registeredBy) {
        validateNotNull(registeredBy, JOB_POSTING_REGISTERED_BY_NULL);
        validateLongMinSize(registeredBy, MIN_REGISTERED_BY_SIZE, JOB_POSTING_INVALID_REGISTERED_BY_SIZE);
    }

    /**
     * 화면에 보여 줄 순서가 1부터 빠짐없이 이어지는지 함께 확인한다.
     */
    private static void validateRequirements(List<JobRequirement> requirements) {
        validateNotNull(requirements, JOB_POSTING_INVALID_REQUIREMENT_COUNT);
        validateIntRange(
                requirements.size(), MIN_REQUIREMENT_COUNT, MAX_REQUIREMENT_COUNT, JOB_POSTING_INVALID_REQUIREMENT_COUNT);
        for (int index = 0; index < requirements.size(); index++) {
            if (requirements.get(index).getDisplayOrder() != index + MIN_DISPLAY_ORDER) {
                throw new DomainValidationException(JOB_POSTING_INVALID_REQUIREMENT_ORDER);
            }
        }
    }

    private static void validateRequirementName(String name) {
        validateNotNullOrBlank(name, JOB_REQUIREMENT_NAME_NULL_OR_BLANK);
        validateMaxLength(name, MAX_REQUIREMENT_NAME_LENGTH, JOB_REQUIREMENT_INVALID_NAME_LENGTH);
    }

    private static void validateRequirementSummary(String summary) {
        validateNotNullOrBlank(summary, JOB_REQUIREMENT_SUMMARY_NULL_OR_BLANK);
        validateMaxLength(summary, MAX_REQUIREMENT_SUMMARY_LENGTH, JOB_REQUIREMENT_INVALID_SUMMARY_LENGTH);
    }
}

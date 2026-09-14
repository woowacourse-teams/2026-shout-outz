package com.shoutoutz.api.project.domain;

import static com.shoutoutz.api.common.validator.DomainValidator.validateMaxLength;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNull;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNullOrBlank;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_COHORT_NULL;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_INVALID_DESCRIPTION_LENGTH;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_INVALID_TAGLINE_LENGTH;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_REGISTERED_BY_NULL;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_TAGLINE_NULL_OR_BLANK;

import com.shoutoutz.api.cohort.domain.Cohort;

/**
 * 프로젝트 도메인 전용 Validator
 * 값 객체로 감싸지 않은 필드만 다룬다. 값 객체의 규칙은 각 값 객체가 갖는다.
 */
final class ProjectValidator {

    private static final int MAX_TAGLINE_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 100_000;

    private ProjectValidator() {
    }

    /**
     * 모든 프로젝트에 적용되는 규칙
     */
    static void validateProject(
            Cohort cohort,
            String tagline,
            String descriptionMd
    ) {
        validateNotNull(cohort, PROJECT_COHORT_NULL);
        validateTagline(tagline);
        validateDescriptionMd(descriptionMd);
    }

    /**
     * 신규 등록에만 적용되는 규칙
     * 이관 프로젝트는 등록자가 없어서, 검사하지 않는다.
     */
    static void validateRegistration(Long registeredBy) {
        validateNotNull(registeredBy, PROJECT_REGISTERED_BY_NULL);
    }

    private static void validateTagline(String tagline) {
        validateNotNullOrBlank(tagline, PROJECT_TAGLINE_NULL_OR_BLANK);
        validateMaxLength(tagline, MAX_TAGLINE_LENGTH, PROJECT_INVALID_TAGLINE_LENGTH);
    }

    private static void validateDescriptionMd(String descriptionMd) {
        if (descriptionMd == null) {
            return;
        }
        validateMaxLength(descriptionMd, MAX_DESCRIPTION_LENGTH, PROJECT_INVALID_DESCRIPTION_LENGTH);
    }
}

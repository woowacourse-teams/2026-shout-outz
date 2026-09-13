package com.shoutoutz.api.user.domain.profile;

import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNull;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNullOrBlank;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.COACH_COHORT_NOT_ALLOWED;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.CREW_COURSE_INFO_REQUIRED;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.DISPLAY_NAME_REQUIRED;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.GENERAL_USER_COURSE_INFO_NOT_ALLOWED;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.PROFILE_DISPLAY_NAME_IMMUTABLE;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.USER_ID_REQUIRED;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.USER_TYPE_REQUIRED;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;

/**
 * UserProfile이 지켜야 하는 필수값, 상태 조합 및 변경 규칙 검증.
 */
final class UserProfileValidator {

    private UserProfileValidator() {
    }

    static void validateDisplayName(String displayName) {
        validateNotNullOrBlank(displayName, DISPLAY_NAME_REQUIRED);
    }

    static void validateDisplayNameChange(
            UserType userType,
            String currentDisplayName,
            String requestedDisplayName
    ) {
        if (userType != UserType.GENERAL && !currentDisplayName.equals(requestedDisplayName)) {
            throw new BadRequestException(PROFILE_DISPLAY_NAME_IMMUTABLE);
        }
    }

    static void validateProfile(
            Long userId,
            UserType userType,
            String track,
            Short cohort
    ) {
        validateNotNull(userId, USER_ID_REQUIRED);
        validateNotNull(userType, USER_TYPE_REQUIRED);
        validateCourseInformation(userType, track, cohort);
    }

    private static void validateCourseInformation(UserType userType, String track, Short cohort) {
        if (userType == UserType.GENERAL && (track != null || cohort != null)) {
            throw new DomainValidationException(GENERAL_USER_COURSE_INFO_NOT_ALLOWED);
        }
        if (userType == UserType.WOOWACOURSE_CREW && (track == null || cohort == null)) {
            throw new DomainValidationException(CREW_COURSE_INFO_REQUIRED);
        }
        if (userType == UserType.WOOWACOURSE_COACH && cohort != null) {
            throw new DomainValidationException(COACH_COHORT_NOT_ALLOWED);
        }
    }

}

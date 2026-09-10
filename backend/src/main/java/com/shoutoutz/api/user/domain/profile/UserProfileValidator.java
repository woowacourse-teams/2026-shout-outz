package com.shoutoutz.api.user.domain.profile;

import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNull;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNullOrBlank;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.COACH_COHORT_NOT_ALLOWED;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.CREW_COURSE_INFO_REQUIRED;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.DISPLAY_NAME_CHANGE_NOT_ALLOWED;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.DISPLAY_NAME_REQUIRED;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.GENERAL_USER_COURSE_INFO_NOT_ALLOWED;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.USER_ID_REQUIRED;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.USER_TYPE_REQUIRED;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;

final class UserProfileValidator {

    private UserProfileValidator() {
    }

    static void validateDisplayName(String displayName) {
        validateNotNullOrBlank(displayName, DISPLAY_NAME_REQUIRED);
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

    static void validateDisplayNameChange(
            UserType userType,
            String currentDisplayName,
            String requestedDisplayName
    ) {
        if (userType != UserType.GENERAL && !currentDisplayName.equals(requestedDisplayName)) {
            throw new DomainValidationException(DISPLAY_NAME_CHANGE_NOT_ALLOWED);
        }
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

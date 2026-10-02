package com.shoutoutz.api.verification.domain;

import static com.shoutoutz.api.verification.domain.UserVerificationErrorCode.VERIFICATION_COURSE_INFO_INVALID;
import static com.shoutoutz.api.verification.domain.UserVerificationErrorCode.VERIFICATION_TRACK_INVALID;
import static com.shoutoutz.api.verification.domain.UserVerificationErrorCode.VERIFICATION_USER_TYPE_INVALID;

import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;
import java.util.Set;

final class UserVerificationRequestValidator {

    private static final Set<String> ALLOWED_TRACKS = Set.of("BACKEND", "FRONTEND", "ANDROID");

    private UserVerificationRequestValidator() {
    }

    static void validate(
            long userId,
            UserType userType,
            String nickname,
            Integer cohort,
            String track,
            Instant requestedAt
    ) {
        if (userId < 1 || requestedAt == null) {
            throw new DomainValidationException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
        if (userType != UserType.WOOWACOURSE_CREW
                && userType != UserType.WOOWACOURSE_COACH) {
            throw new BadRequestException(VERIFICATION_USER_TYPE_INVALID);
        }
        if (nickname == null || nickname.isBlank()
                || nickname.codePointCount(0, nickname.length()) > 50) {
            throw new BadRequestException(CommonErrorCode.VALIDATION_FAILED);
        }
        if (cohort != null && cohort <= 0) {
            throw new BadRequestException(CommonErrorCode.VALIDATION_FAILED);
        }

        if (userType == UserType.WOOWACOURSE_CREW) {
            if (cohort == null || track == null || track.isBlank()) {
                throw new BadRequestException(VERIFICATION_COURSE_INFO_INVALID);
            }
            if (!ALLOWED_TRACKS.contains(track)) {
                throw new BadRequestException(VERIFICATION_TRACK_INVALID);
            }
            return;
        }

        if (cohort != null || track != null) {
            throw new BadRequestException(VERIFICATION_COURSE_INFO_INVALID);
        }
    }
}

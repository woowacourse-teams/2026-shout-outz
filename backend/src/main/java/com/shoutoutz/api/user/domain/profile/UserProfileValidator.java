package com.shoutoutz.api.user.domain.profile;

import static com.shoutoutz.api.common.validator.DomainValidator.validateLongMinSize;
import static com.shoutoutz.api.common.validator.DomainValidator.validateMaxLength;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNull;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNullOrBlank;
import static com.shoutoutz.api.common.validator.DomainValidator.validatePattern;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.AVATAR_IMAGE_ID_INVALID;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.BIO_TOO_LONG;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.BLOG_URL_INVALID;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.COACH_COHORT_NOT_ALLOWED;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.CREW_COURSE_INFO_REQUIRED;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.DISPLAY_NAME_CHANGE_NOT_ALLOWED;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.DISPLAY_NAME_REQUIRED;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.DISPLAY_NAME_TOO_LONG;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.GENERAL_USER_COURSE_INFO_NOT_ALLOWED;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.GITHUB_PROFILE_URL_INVALID;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.USER_ID_REQUIRED;
import static com.shoutoutz.api.user.domain.profile.UserProfileErrorCode.USER_TYPE_REQUIRED;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.util.regex.Pattern;

final class UserProfileValidator {

    private static final int MAX_DISPLAY_NAME_LENGTH = 50;
    private static final int MAX_BIO_LENGTH = 200;
    private static final Pattern GITHUB_PROFILE_URL_PATTERN = Pattern.compile(
            "^https://github\\.com/[^/\\s?#]+/?$"
    );
    private static final Pattern BLOG_URL_PATTERN = Pattern.compile("^https?://[^\\s]+$");

    private UserProfileValidator() {
    }

    static void validateDisplayName(String displayName) {
        validateNotNullOrBlank(displayName, DISPLAY_NAME_REQUIRED);
        validateMaxLength(displayName, MAX_DISPLAY_NAME_LENGTH, DISPLAY_NAME_TOO_LONG);
    }

    static void validateProfile(
            Long userId,
            UserType userType,
            String track,
            Short cohort,
            String bio,
            Long avatarImageId,
            String githubProfileUrl,
            String blogUrl
    ) {
        validateNotNull(userId, USER_ID_REQUIRED);
        validateNotNull(userType, USER_TYPE_REQUIRED);
        validateCourseInformation(userType, track, cohort);
        validateBio(bio);
        validateAvatarImageId(avatarImageId);
        validateGithubProfileUrl(githubProfileUrl);
        validateBlogUrl(blogUrl);
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

    private static void validateBio(String bio) {
        if (bio != null) {
            validateMaxLength(bio, MAX_BIO_LENGTH, BIO_TOO_LONG);
        }
    }

    private static void validateAvatarImageId(Long avatarImageId) {
        if (avatarImageId != null) {
            validateLongMinSize(avatarImageId, 1, AVATAR_IMAGE_ID_INVALID);
        }
    }

    private static void validateGithubProfileUrl(String githubProfileUrl) {
        if (githubProfileUrl != null) {
            validatePattern(githubProfileUrl, GITHUB_PROFILE_URL_PATTERN, GITHUB_PROFILE_URL_INVALID);
        }
    }

    private static void validateBlogUrl(String blogUrl) {
        if (blogUrl != null) {
            validatePattern(blogUrl, BLOG_URL_PATTERN, BLOG_URL_INVALID);
        }
    }
}

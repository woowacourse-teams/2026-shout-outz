package com.shoutoutz.api.user.domain.account;

import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNull;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNullOrBlank;
import static com.shoutoutz.api.user.domain.account.UserErrorCode.USER_DELETION_STATE_INVALID;
import static com.shoutoutz.api.user.domain.account.UserErrorCode.USER_HANDLE_REQUIRED;
import static com.shoutoutz.api.user.domain.account.UserErrorCode.USER_LOGIN_AT_REQUIRED;
import static com.shoutoutz.api.user.domain.account.UserErrorCode.USER_LOGIN_BANNED;
import static com.shoutoutz.api.user.domain.account.UserErrorCode.USER_LOGIN_PURGED;
import static com.shoutoutz.api.user.domain.account.UserErrorCode.USER_ROLE_REQUIRED;
import static com.shoutoutz.api.user.domain.account.UserErrorCode.USER_STATUS_REQUIRED;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.time.Instant;

/**
 * User와 Handle의 생성 및 변경 규칙 검증.
 */
final class UserValidator {

    private UserValidator() {
    }

    static void validateUser(UserStatus status, UserRole role, Instant deletedAt) {
        validateNotNull(status, USER_STATUS_REQUIRED);
        validateNotNull(role, USER_ROLE_REQUIRED);

        if ((status == UserStatus.DELETED) != (deletedAt != null)) {
            throw new DomainValidationException(USER_DELETION_STATE_INVALID);
        }
    }

    static void validateHandle(String handle) {
        validateNotNullOrBlank(handle, USER_HANDLE_REQUIRED);
    }

    static void validateLogin(UserStatus status, Instant purgedAt, Instant loginAt) {
        if (status == UserStatus.BANNED) {
            throw new DomainValidationException(USER_LOGIN_BANNED);
        }
        if (purgedAt != null) {
            throw new DomainValidationException(USER_LOGIN_PURGED);
        }
        validateNotNull(loginAt, USER_LOGIN_AT_REQUIRED);
    }
}

package com.shoutoutz.api.user.exception;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND("사용자를 찾을 수 없습니다."),
    USER_PROFILE_NOT_FOUND("사용자 프로필을 찾을 수 없습니다.");

    private final String message;
}

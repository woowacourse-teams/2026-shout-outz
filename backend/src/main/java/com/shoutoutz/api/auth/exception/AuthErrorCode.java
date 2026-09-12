package com.shoutoutz.api.auth.exception;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    OAUTH_LOGIN_SESSION_NOT_FOUND("OAuth 로그인 세션을 찾을 수 없습니다."),
    OAUTH_LOGIN_ATTEMPT_NOT_FOUND("OAuth 로그인 시도를 찾을 수 없습니다."),
    OAUTH_SIGNUP_SESSION_NOT_FOUND("가입 대기 OAuth 신원을 찾을 수 없습니다."),
    CSRF_TOKEN_INVALID("CSRF Token이 유효하지 않습니다.");

    private final String message;
}

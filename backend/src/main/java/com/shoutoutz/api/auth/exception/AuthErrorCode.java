package com.shoutoutz.api.auth.exception;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    OAUTH_LOGIN_IN_PROGRESS("이미 OAuth 로그인이 진행 중입니다."),
    OAUTH_STATE_MISMATCH("OAuth state가 일치하지 않습니다."),
    OAUTH_LOGIN_ATTEMPT_EXPIRED("OAuth 로그인 시도가 만료되었습니다."),
    OAUTH_LOGIN_SESSION_NOT_FOUND("OAuth 로그인 세션을 찾을 수 없습니다."),
    OAUTH_LOGIN_ATTEMPT_NOT_FOUND("OAuth 로그인 시도를 찾을 수 없습니다."),
    OAUTH_COMPLETION_URI_NOT_ALLOWED("허용되지 않은 OAuth 완료 URI입니다."),
    OAUTH_SIGNUP_SESSION_NOT_FOUND("가입 대기 OAuth 신원을 찾을 수 없습니다."),
    CSRF_TOKEN_INVALID("CSRF Token이 유효하지 않습니다."),

    // [부하 테스트 전용] loadtest 프로필의 로그인 API에서만 사용
    LOAD_TEST_LOGIN_NOT_ALLOWED("부하 테스트 로그인은 활성 상태의 일반 사용자만 가능합니다.");

    private final String message;
}

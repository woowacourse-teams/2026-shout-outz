package com.shoutoutz.api.common.exception.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    VALIDATION_FAILED("입력값을 확인해주세요."),
    UNAUTHORIZED("인증이 필요합니다."),
    FORBIDDEN("접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND("자원을 찾을 수 없습니다."),
    DUPLICATE_RESOURCE("자원이 이미 존재합니다."),
    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다.");

    private final String message;
}

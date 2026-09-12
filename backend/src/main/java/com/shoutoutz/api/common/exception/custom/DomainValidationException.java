package com.shoutoutz.api.common.exception.custom;

import com.shoutoutz.api.common.exception.code.ErrorCode;

/**
 * 도메인 규칙을 위반했을 때 발생하는 500 예외입니다.
 * 사용자 입력 검증은 이미 API 레이어에서 검증되어 들어옵니다.
 * 따라서, 서버 내부의 작업으로 인해 발생한 도메인 검증 예외를 표현합니다.
 */
public class DomainValidationException extends InternalServerErrorException {

    public DomainValidationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DomainValidationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

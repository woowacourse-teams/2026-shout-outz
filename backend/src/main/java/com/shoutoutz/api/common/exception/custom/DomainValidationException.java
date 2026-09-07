package com.shoutoutz.api.common.exception.custom;

import com.shoutoutz.api.common.exception.code.ErrorCode;

/**
 * 사용자 입력으로 도메인 규칙을 위반했을 때 발생하는 400 예외입니다.
 * 서버가 생성한 값이나 저장 데이터의 불변식 위반은 InternalServerErrorException으로 구분합니다.
 */
public class DomainValidationException extends BadRequestException {

    public DomainValidationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DomainValidationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

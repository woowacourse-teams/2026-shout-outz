package com.shoutoutz.api.common.exception.custom;

import com.shoutoutz.api.common.exception.code.ErrorCode;

/**
 * 도메인 객체의 생성 또는 상태 변경 과정에서 불변식 검증에 실패했을 때 발생하는 예외입니다.
 */
public class DomainValidationException extends CustomException {

    public DomainValidationException(ErrorCode errorCode) {
        super(errorCode);
    }
}

package com.shoutoutz.api.common.exception.custom;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 중복 또는 자원의 현재 상태 때문에 요청을 처리할 수 없을 때 사용하는 409 예외입니다.
 */
public class ConflictException extends CustomException {

    public ConflictException(ErrorCode errorCode) {
        super(HttpStatus.CONFLICT, errorCode);
    }

    public ConflictException(ErrorCode errorCode, Throwable cause) {
        super(HttpStatus.CONFLICT, errorCode, cause);
    }
}

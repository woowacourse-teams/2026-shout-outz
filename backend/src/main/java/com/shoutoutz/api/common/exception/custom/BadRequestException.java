package com.shoutoutz.api.common.exception.custom;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 요청값이 유효하지 않을 때 사용하는 400 예외입니다.
 */
public class BadRequestException extends CustomException {

    public BadRequestException(ErrorCode errorCode) {
        super(HttpStatus.BAD_REQUEST, errorCode);
    }

    public BadRequestException(ErrorCode errorCode, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, errorCode, cause);
    }
}

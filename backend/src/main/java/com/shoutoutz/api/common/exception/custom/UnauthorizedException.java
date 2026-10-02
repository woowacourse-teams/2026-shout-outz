package com.shoutoutz.api.common.exception.custom;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 인증이 필요하거나 인증 정보가 유효하지 않을 때 사용하는 401 예외입니다.
 */
public class UnauthorizedException extends CustomException {

    public UnauthorizedException(ErrorCode errorCode) {
        super(HttpStatus.UNAUTHORIZED, errorCode);
    }

    public UnauthorizedException(ErrorCode errorCode, Throwable cause) {
        super(HttpStatus.UNAUTHORIZED, errorCode, cause);
    }
}

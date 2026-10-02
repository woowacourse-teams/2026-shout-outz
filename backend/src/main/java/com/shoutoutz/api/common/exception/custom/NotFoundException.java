package com.shoutoutz.api.common.exception.custom;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 요청한 자원을 찾을 수 없을 때 사용하는 404 예외입니다.
 */
public class NotFoundException extends CustomException {

    public NotFoundException(ErrorCode errorCode) {
        super(HttpStatus.NOT_FOUND, errorCode);
    }

    public NotFoundException(ErrorCode errorCode, Throwable cause) {
        super(HttpStatus.NOT_FOUND, errorCode, cause);
    }
}

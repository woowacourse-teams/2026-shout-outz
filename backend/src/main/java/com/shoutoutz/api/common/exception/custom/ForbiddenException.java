package com.shoutoutz.api.common.exception.custom;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 요청한 작업을 수행할 권한이 없을 때 사용하는 403 예외입니다.
 */
public class ForbiddenException extends CustomException {

    public ForbiddenException(ErrorCode errorCode) {
        super(HttpStatus.FORBIDDEN, errorCode);
    }

    public ForbiddenException(ErrorCode errorCode, Throwable cause) {
        super(HttpStatus.FORBIDDEN, errorCode, cause);
    }
}

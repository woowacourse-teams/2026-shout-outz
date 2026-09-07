package com.shoutoutz.api.common.exception.custom;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * HTTP 상태와 오류 정보를 보유하는 커스텀 예외의 공통 부모 클래스입니다.
 * 상태별 예외를 직접 사용하거나 상속하여 구체적인 예외를 정의합니다.
 */
public abstract class CustomException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final ErrorCode errorCode;

    protected CustomException(HttpStatus httpStatus, ErrorCode errorCode) {
        this(httpStatus, errorCode, null);
    }

    protected CustomException(HttpStatus httpStatus, ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

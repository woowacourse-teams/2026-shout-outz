package com.shoutoutz.api.common.exception.custom;

import com.shoutoutz.api.common.exception.code.ErrorCode;

/**
 * 올바르지 않은 입력 값인 경우 발생하는 예외
 */
public class InvalidInputException extends BadRequestException {
    public InvalidInputException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidInputException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

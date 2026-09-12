package com.shoutoutz.api.common.exception.custom;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 서버 내부의 잘못된 상태나 처리 실패를 나타내는 500 예외입니다.
 */
public class InternalServerErrorException extends CustomException {

    public InternalServerErrorException(ErrorCode errorCode) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, errorCode);
    }

    public InternalServerErrorException(ErrorCode errorCode, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, errorCode, cause);
    }
}

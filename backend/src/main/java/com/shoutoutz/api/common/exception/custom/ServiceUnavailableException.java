package com.shoutoutz.api.common.exception.custom;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 외부 서비스의 장애나 시간 초과로 요청을 처리하지 못했음을 나타내는 503 예외입니다.
 */
public class ServiceUnavailableException extends CustomException {

    public ServiceUnavailableException(ErrorCode errorCode) {
        super(HttpStatus.SERVICE_UNAVAILABLE, errorCode);
    }

    public ServiceUnavailableException(ErrorCode errorCode, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, errorCode, cause);
    }
}

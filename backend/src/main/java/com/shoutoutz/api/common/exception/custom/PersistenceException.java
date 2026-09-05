package com.shoutoutz.api.common.exception.custom;

import com.shoutoutz.api.common.exception.code.ErrorCode;

/**
 * 저장소 또는 데이터베이스 처리 중 예상하지 못한 오류가 발생했을 때 사용하는 예외입니다.
 */
public class PersistenceException extends CustomException {

    public PersistenceException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

package com.shoutoutz.api.common.exception.custom;

import com.shoutoutz.api.common.exception.code.ErrorCode;

/**
 * 생성 또는 저장하려는 엔티티가 이미 존재할 때 발생하는 예외입니다.
 */
public class DuplicateEntityException extends CustomException {

    public DuplicateEntityException(ErrorCode errorCode) {
        super(errorCode);
    }
}

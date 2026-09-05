package com.shoutoutz.api.common.exception.custom;

import com.shoutoutz.api.common.exception.code.ErrorCode;

/**
 * 조회 또는 변경하려는 엔티티를 찾지 못했을 때 발생하는 예외입니다.
 */
public class EntityNotFoundException extends CustomException {

    public EntityNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}

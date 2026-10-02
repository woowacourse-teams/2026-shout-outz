package com.shoutoutz.api.cohort.domain;

import com.shoutoutz.api.common.exception.custom.BadRequestException;

/**
 * 정의되지 않은 우아한테크코스 기수 번호로 기수를 찾을 때 발생하는 예외입니다.
 */
public class InvalidCohortException extends BadRequestException {

    public InvalidCohortException() {
        super(CohortErrorCode.INVALID_COHORT);
    }
}

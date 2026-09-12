package com.shoutoutz.api.cohort.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CohortErrorCode implements ErrorCode {
    /**
     * 기수 (Cohort) 에러 코드
     */
    INVALID_COHORT("정의되지 않은 우아한테크코스 기수입니다.");

    private final String message;
}

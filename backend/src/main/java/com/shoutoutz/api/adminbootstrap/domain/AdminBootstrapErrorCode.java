package com.shoutoutz.api.adminbootstrap.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminBootstrapErrorCode implements ErrorCode {
    CODE_INVALID("관리자 권한 부여 코드가 유효하지 않습니다.");

    private final String message;
}

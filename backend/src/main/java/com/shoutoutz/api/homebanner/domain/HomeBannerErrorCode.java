package com.shoutoutz.api.homebanner.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HomeBannerErrorCode implements ErrorCode {
    HOME_BANNER_INVALID_STATE("홈 배너 정보가 올바르지 않습니다.");

    private final String message;
}

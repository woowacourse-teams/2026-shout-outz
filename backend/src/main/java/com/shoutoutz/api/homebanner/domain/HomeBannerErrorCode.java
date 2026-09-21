package com.shoutoutz.api.homebanner.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HomeBannerErrorCode implements ErrorCode {
    HOME_BANNER_INVALID_STATE("홈 배너 정보가 올바르지 않습니다."),
    HOME_BANNER_ADMIN_FORBIDDEN("홈 배너 관리 권한이 없습니다."),
    HOME_BANNER_NOT_FOUND("홈 배너를 찾을 수 없습니다."),
    HOME_BANNER_MEDIA_NOT_FOUND("홈 배너 미디어를 찾을 수 없습니다."),
    HOME_BANNER_MEDIA_INVALID("READY 상태의 HOME_BANNER 미디어만 사용할 수 있습니다."),
    HOME_BANNER_TARGET_NOT_FOUND("홈 배너가 가리키는 대상을 찾을 수 없습니다.");

    private final String message;
}

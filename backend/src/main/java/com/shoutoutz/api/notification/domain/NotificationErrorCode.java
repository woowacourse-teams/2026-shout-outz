package com.shoutoutz.api.notification.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
    NOTIFICATION_NOT_FOUND("요청한 알림을 찾을 수 없습니다."),
    INVALID_NOTIFICATION_CURSOR("올바르지 않은 알림 커서입니다."),
    INVALID_NOTIFICATION_SIZE("알림 조회 개수는 1에서 50 사이여야 합니다.");

    private final String message;
}

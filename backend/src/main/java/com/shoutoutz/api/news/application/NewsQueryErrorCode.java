package com.shoutoutz.api.news.application;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NewsQueryErrorCode implements ErrorCode {
    NEWS_INVALID_TYPE("소식 유형 필터가 올바르지 않습니다."),
    NEWS_INVALID_EVENT_STATUS("이벤트 상태 필터가 올바르지 않습니다."),
    NEWS_INVALID_SORT("소식 정렬 기준이 올바르지 않습니다."),
    NEWS_INVALID_SIZE("소식 조회 개수는 1 이상 50 이하여야 합니다."),
    NEWS_INVALID_CURSOR("소식 목록 조회 커서가 올바르지 않습니다.");

    private final String message;
}

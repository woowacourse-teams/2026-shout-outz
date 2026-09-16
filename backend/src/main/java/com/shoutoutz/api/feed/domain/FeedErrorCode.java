package com.shoutoutz.api.feed.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeedErrorCode implements ErrorCode {
    FEED_INVALID_STATE("피드 상태가 올바르지 않습니다."),
    FEED_NOT_FOUND("피드를 찾을 수 없습니다."),
    FEED_AUTHOR_FORBIDDEN("피드 작성자만 요청할 수 있습니다."),
    FEED_WRITER_TYPE_FORBIDDEN("크루 또는 코치만 피드를 작성할 수 있습니다."),
    FEED_CATEGORY_INVALID("사용할 수 없는 카테고리가 포함되어 있습니다."),
    FEED_CATEGORY_SELECTION_INVALID("일반 카테고리는 하나만 선택해야 합니다."),
    FEED_MEDIA_INVALID("사용할 수 없는 본문 이미지가 포함되어 있습니다."),
    FEED_CURSOR_INVALID("피드 커서가 올바르지 않습니다.");

    private final String message;
}

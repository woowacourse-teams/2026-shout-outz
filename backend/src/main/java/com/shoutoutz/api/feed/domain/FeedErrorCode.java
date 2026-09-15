package com.shoutoutz.api.feed.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeedErrorCode implements ErrorCode {
    FEED_NOT_FOUND("요청한 피드를 찾을 수 없습니다.");

    private final String message;
}

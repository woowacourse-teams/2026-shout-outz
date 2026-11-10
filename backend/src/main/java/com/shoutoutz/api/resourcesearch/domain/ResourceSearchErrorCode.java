package com.shoutoutz.api.resourcesearch.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResourceSearchErrorCode implements ErrorCode {
    SEARCH_KEYWORD_NULL_OR_BLANK("검색어는 null이거나 빈값일 수 없습니다."),
    SEARCH_KEYWORD_INVALID_LENGTH("검색어는 50자 이하여야 합니다."),
    RESOURCE_NOT_FOUND("자료를 찾을 수 없습니다.");

    private final String message;
}

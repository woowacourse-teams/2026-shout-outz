package com.shoutoutz.api.category.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements ErrorCode {
    CATEGORY_INVALID_STATE("카테고리 상태가 올바르지 않습니다."),
    CATEGORY_NOT_FOUND("카테고리를 찾을 수 없습니다."),
    CATEGORY_ADMIN_FORBIDDEN("관리자만 카테고리를 변경할 수 있습니다."),
    CATEGORY_ALREADY_EXISTS("이미 존재하는 slug 또는 표시 이름입니다.");

    private final String message;
}

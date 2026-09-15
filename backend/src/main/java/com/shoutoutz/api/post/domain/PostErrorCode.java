package com.shoutoutz.api.post.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostErrorCode implements ErrorCode {
    POST_INVALID_STATE("포스트 상태가 올바르지 않습니다."),
    POST_NOT_FOUND("포스트를 찾을 수 없습니다."),
    POST_AUTHOR_FORBIDDEN("포스트 작성자만 요청할 수 있습니다."),
    POST_WRITER_TYPE_FORBIDDEN("크루 또는 코치만 포스트를 작성할 수 있습니다."),
    POST_CATEGORY_INVALID("사용할 수 없는 카테고리가 포함되어 있습니다."),
    POST_CATEGORY_SELECTION_INVALID("일반 카테고리는 하나만 선택해야 합니다."),
    POST_MEDIA_INVALID("사용할 수 없는 본문 이미지가 포함되어 있습니다."),
    POST_CURSOR_INVALID("포스트 커서가 올바르지 않습니다.");

    private final String message;
}

package com.shoutoutz.api.comment.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {
    COMMENT_NOT_FOUND("요청한 댓글을 찾을 수 없습니다."),
    COMMENT_DEPTH_EXCEEDED("대댓글은 한 단계까지만 작성할 수 있습니다.");

    private final String message;
}

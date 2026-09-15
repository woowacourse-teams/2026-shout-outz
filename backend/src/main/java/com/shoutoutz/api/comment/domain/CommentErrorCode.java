package com.shoutoutz.api.comment.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {
    COMMENT_NOT_FOUND("요청한 댓글을 찾을 수 없습니다."),
    COMMENT_DEPTH_EXCEEDED("대댓글은 한 단계까지만 작성할 수 있습니다."),

    INVALID_COMMENT_CURSOR("올바르지 않은 댓글 커서입니다."),
    INVALID_COMMENT_SORT("올바르지 않은 댓글 정렬 기준입니다."),
    INVALID_COMMENT_SIZE("올바르지 않은 댓글 조회 개수입니다."),
    MISMATCHED_COMMENT_SORT_AND_CURSOR_SORT("커서 속 정렬 기준과 입력한 정렬 기준이 서로 다릅니다.");

    private final String message;
}

package com.shoutoutz.api.common.exception.code;

/**
 * 외부에 전달할 오류 코드와 메시지. HTTP 상태는 커스텀 예외가 결정한다.
 */
public interface ErrorCode {
    String name();

    String getMessage();
}

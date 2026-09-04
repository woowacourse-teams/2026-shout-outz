package com.shoutoutz.api.common.exception.event;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;

@Getter
public class DiscordInternalServerErrorEvent {
    private final String message;

    public DiscordInternalServerErrorEvent(String requestUri, ErrorCode errorCode, String message) {
        this.message =
                """
                        ❗️[서버 오류] 500 오류 발생
                        ➤ 요청 URL: %s
                        ➤ 예외 에러코드: %s
                        ➤ 예외 메시지: %s
                        """
                        .formatted(requestUri, errorCode, message);
    }
}

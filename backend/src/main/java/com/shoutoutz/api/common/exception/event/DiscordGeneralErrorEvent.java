package com.shoutoutz.api.common.exception.event;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;

@Getter
public class DiscordGeneralErrorEvent {
    private final String message;

    public DiscordGeneralErrorEvent(String requestUri, ErrorCode errorCode, String message) {
        String maskedRequestUri = DiscordMessageSanitizer.mask(requestUri);
        String maskedMessage = DiscordMessageSanitizer.mask(message);
        this.message =
                """
                        ❗️[서버 오류] %s 오류 발생
                        ➤ 요청 URL: %s
                        ➤ 예외 에러코드: %s
                        ➤ 예외 메시지: %s
                        """
                        .formatted(errorCode.getHttpStatus().value(), maskedRequestUri, errorCode, maskedMessage);
    }
}

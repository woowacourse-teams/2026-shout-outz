package com.shoutoutz.api.media.application.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 요청자가 미디어를 조회할 권한이 없을 때 발생한다.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class MediaAccessForbiddenException extends RuntimeException {

    public MediaAccessForbiddenException(String message) {
        super(message);
    }
}

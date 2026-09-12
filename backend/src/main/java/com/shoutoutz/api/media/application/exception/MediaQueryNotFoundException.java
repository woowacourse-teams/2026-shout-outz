package com.shoutoutz.api.media.application.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 조회 대상 미디어가 존재하지 않을 때 발생한다.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class MediaQueryNotFoundException extends RuntimeException {

    public MediaQueryNotFoundException(long mediaId) {
        super("미디어를 찾을 수 없습니다: " + mediaId);
    }
}

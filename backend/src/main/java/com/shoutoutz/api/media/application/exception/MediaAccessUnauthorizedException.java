package com.shoutoutz.api.media.application.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 인증이 필요한 미디어 조회 요청에 인증 정보가 없을 때 발생한다.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class MediaAccessUnauthorizedException extends RuntimeException {

    public MediaAccessUnauthorizedException(String message) {
        super(message);
    }
}

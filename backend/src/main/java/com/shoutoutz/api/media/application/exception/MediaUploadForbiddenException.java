package com.shoutoutz.api.media.application.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 요청자가 미디어 업로드 대상에 대한 수정 권한을 갖지 않을 때 발생한다.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class MediaUploadForbiddenException extends RuntimeException {

    public MediaUploadForbiddenException(String message) {
        super(message);
    }
}

package com.shoutoutz.api.media.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 미디어 업로드를 완료할 수 없는 현재 상태일 때 발생한다.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class MediaUploadCompletionConflictException extends RuntimeException {

    public MediaUploadCompletionConflictException(String message) {
        super(message);
    }
}

package com.shoutoutz.api.media.application.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * S3 객체의 업로드 메타데이터가 요청 정보와 다를 때 발생한다.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class MediaUploadCompletionValidationException extends RuntimeException {

    public MediaUploadCompletionValidationException(String message) {
        super(message);
    }
}

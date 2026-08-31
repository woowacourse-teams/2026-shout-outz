package com.shoutoutz.api.media.infrastructure.s3;

/**
 * S3에 요청한 미디어 객체가 존재하지 않을 때 발생한다.
 */
public class S3ObjectNotFoundException extends RuntimeException {

    public S3ObjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

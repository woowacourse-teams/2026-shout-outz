package com.shoutoutz.api.media.infrastructure.s3;

/**
 * S3 SDK 호출이 실패했을 때 인프라 예외를 추상화한다.
 */
public class S3StorageException extends RuntimeException {

    public S3StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

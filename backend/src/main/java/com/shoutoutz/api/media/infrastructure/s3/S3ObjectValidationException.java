package com.shoutoutz.api.media.infrastructure.s3;

/**
 * S3 객체의 실제 메타데이터가 업로드 요청 정보와 다를 때 발생한다.
 */
public class S3ObjectValidationException extends RuntimeException {

    public S3ObjectValidationException(String message) {
        super(message);
    }
}

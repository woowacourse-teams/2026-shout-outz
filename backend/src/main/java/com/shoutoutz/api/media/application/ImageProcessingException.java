package com.shoutoutz.api.media.application;

/**
 * 이미지 처리 실패를 외부 노출용 메시지와 분리해 전달한다.
 */
public class ImageProcessingException extends RuntimeException {

    private final String failureReason;

    public ImageProcessingException(String failureReason) {
        super(failureReason);
        this.failureReason = failureReason;
    }

    public ImageProcessingException(String failureReason, Throwable cause) {
        super(failureReason, cause);
        this.failureReason = failureReason;
    }

    public String failureReason() {
        return failureReason;
    }
}

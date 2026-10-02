package com.shoutoutz.api.media.domain;

import java.util.Set;

/**
 * 미디어 업로드 정책 인터페이스 계약
 * @author josangjun
 */
public interface MediaUploadPolicy {

    long maxImageSizeBytes();

    Set<String> allowedImageContentTypes();

    /**
     * 이미지 업로드 요청을 검증한다.
     *
     * 확장자는 신뢰하지 않고, 이후 S3 객체 검증 단계에서 실제 파일 시그니처도 확인한다.
     */
    void validateImage(String contentType, long sizeBytes);
}

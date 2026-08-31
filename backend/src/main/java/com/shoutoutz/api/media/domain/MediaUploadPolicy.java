package com.shoutoutz.api.media.domain;

import java.util.Set;

/**
 * 미디어 업로드 정책의 계약
 *
 * <p>업로드를 사용하는 코드는 구체적인 정책 구현체가 아니라 이 인터페이스에 의존한다.
 * 정책 변경 시 구현체를 교체할 수 있다.</p>
 */
public interface MediaUploadPolicy {

    long maxImageSizeBytes();

    Set<String> allowedImageContentTypes();

    /**
     * 이미지 업로드 요청을 검증한다.
     *
     * <p>확장자는 신뢰하지 않고, 이후 S3 객체 검증 단계에서 실제 파일 시그니처도 확인한다.</p>
     */
    void validateImage(String contentType, long sizeBytes);
}

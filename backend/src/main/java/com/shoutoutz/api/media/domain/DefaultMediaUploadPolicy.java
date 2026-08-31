package com.shoutoutz.api.media.domain;

import java.util.Locale;
import java.util.Set;

/**
 * 초기 이미지 업로드 정책 구현체
 */
public final class DefaultMediaUploadPolicy implements MediaUploadPolicy {

    static final long MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024;

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    @Override
    public long maxImageSizeBytes() {
        return MAX_IMAGE_SIZE_BYTES;
    }

    @Override
    public Set<String> allowedImageContentTypes() {
        return ALLOWED_IMAGE_CONTENT_TYPES;
    }

    @Override
    public void validateImage(String contentType, long sizeBytes) {
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("이미지 Content-Type은 필수입니다.");
        }
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("이미지 파일 크기는 0보다 커야 합니다.");
        }
        if (sizeBytes > maxImageSizeBytes()) {
            throw new IllegalArgumentException("이미지 파일은 10MiB 이하여야 합니다.");
        }

        String normalizedContentType = contentType.trim().toLowerCase(Locale.ROOT);
        if (!allowedImageContentTypes().contains(normalizedContentType)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 Content-Type입니다: " + contentType);
        }
    }
}

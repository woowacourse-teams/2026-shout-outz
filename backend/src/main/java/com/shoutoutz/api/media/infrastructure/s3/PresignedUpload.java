package com.shoutoutz.api.media.infrastructure.s3;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/**
 * 프론트엔드가 S3에 직접 업로드할 때 사용하는 Presigned PUT 정보
 */
public record PresignedUpload(
        String key,
        URI url,
        Instant expiresAt,
        String contentType
) {

    public PresignedUpload {
        key = Objects.requireNonNull(key, "S3 객체 키는 필수입니다.");
        url = Objects.requireNonNull(url, "Presigned URL은 필수입니다.");
        expiresAt = Objects.requireNonNull(expiresAt, "Presigned URL 만료 시각은 필수입니다.");
        contentType = Objects.requireNonNull(contentType, "Content-Type은 필수입니다.");
    }
}

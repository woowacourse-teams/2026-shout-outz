package com.shoutoutz.api.media.infrastructure.s3;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/**
 * 비공개 S3 객체를 조회할 때 사용하는 Presigned GET 정보
 */
public record PresignedDownload(
        String key,
        URI url,
        Instant expiresAt
) {

    public PresignedDownload {
        key = Objects.requireNonNull(key, "S3 객체 키는 필수입니다.");
        url = Objects.requireNonNull(url, "Presigned URL은 필수입니다.");
        expiresAt = Objects.requireNonNull(expiresAt, "Presigned URL 만료 시각은 필수입니다.");
    }
}

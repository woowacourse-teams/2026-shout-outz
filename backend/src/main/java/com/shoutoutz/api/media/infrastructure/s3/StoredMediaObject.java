package com.shoutoutz.api.media.infrastructure.s3;

import java.time.Instant;
import java.util.Locale;

/**
 * S3 HeadObject 응답에서 미디어 검증에 필요한 정보
 */
public record StoredMediaObject(
        String key,
        long sizeBytes,
        String contentType,
        String eTag,
        Instant lastModified
) {

    public StoredMediaObject {
        contentType = contentType == null ? "" : contentType.strip().toLowerCase(Locale.ROOT);
    }
}

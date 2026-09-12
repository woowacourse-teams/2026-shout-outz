package com.shoutoutz.api.media.presentation.dto.response;

import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import java.net.URI;
import java.time.Instant;

/**
 * 미디어 변형본을 일시적으로 조회할 Presigned GET URL 응답.
 */
public record MediaDownloadResponse(
        Long mediaId,
        MediaVariant variant,
        URI downloadUrl,
        Instant expiresAt,
        String contentType
) {
}

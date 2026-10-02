package com.shoutoutz.api.media.presentation.dto.response;

import com.shoutoutz.api.media.domain.MediaStatus;
import java.net.URI;
import java.time.Instant;

/**
 * 미디어 업로드 시작 응답
 */
public record MediaUploadStartResponse(
        Long mediaId,
        MediaStatus status,
        URI uploadUrl,
        Instant expiresAt,
        String contentType
) {
}

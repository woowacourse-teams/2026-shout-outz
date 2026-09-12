package com.shoutoutz.api.media.presentation.dto.response;

import com.shoutoutz.api.media.domain.MediaStatus;
import java.time.Instant;

/**
 * 미디어 업로드 완료 응답
 */
public record MediaUploadCompleteResponse(
        Long mediaId,
        MediaStatus status,
        long sizeBytes,
        String contentType,
        Instant uploadedAt
) {
}

package com.shoutoutz.api.media.presentation.dto.response;

import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import java.net.URI;

/**
 * 미디어 변형본을 제공하는 공개 URL 응답.
 */
public record MediaDownloadResponse(
        MediaVariant variant,
        URI url,
        String contentType
) {
}

package com.shoutoutz.api.media.application;

/**
 * 이미지 처리 결과 중 미디어 메타데이터에 반영할 값.
 */
public record MediaImageProcessingResult(
        long originalSizeBytes
) {
}

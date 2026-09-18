package com.shoutoutz.api.media.presentation.dto.request;

import com.shoutoutz.api.media.domain.MediaPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 미디어 업로드 시작 요청
 */
public record MediaUploadStartRequest(
        @NotNull MediaPurpose purpose,
        @NotBlank @Size(max = 255) String originalFileName,
        @NotBlank String contentType,
        @Positive long sizeBytes
) {
}

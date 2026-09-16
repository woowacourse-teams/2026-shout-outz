package com.shoutoutz.api.media.presentation.dto.request;

import com.shoutoutz.api.media.domain.MediaPurpose;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 미디어 업로드 시작 요청
 */
public record MediaUploadStartRequest(
        @NotNull MediaPurpose purpose,
        @Positive Long targetId,
        @NotBlank @Size(max = 255) String originalFileName,
        @NotBlank String contentType,
        @Positive long sizeBytes
) {

    @AssertTrue(message = "HOME_BANNER는 targetId가 없어야 하며, 다른 목적은 targetId가 필수입니다.")
    public boolean isTargetIdValid() {
        if (purpose == null) {
            return true;
        }
        return purpose == MediaPurpose.HOME_BANNER
                ? targetId == null
                : targetId != null;
    }
}

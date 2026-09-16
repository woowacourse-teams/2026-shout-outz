package com.shoutoutz.api.media.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.media.domain.MediaPurpose;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class MediaUploadStartRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 홈_배너는_targetId_없이_업로드할_수_있다() {
        MediaUploadStartRequest request = request(MediaPurpose.HOME_BANNER, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 홈_배너에_targetId가_있으면_거부한다() {
        MediaUploadStartRequest request = request(MediaPurpose.HOME_BANNER, 1L);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void 기존_목적은_targetId가_필수다() {
        MediaUploadStartRequest request = request(MediaPurpose.FEED_CONTENT, null);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void 기존_목적은_양수_targetId를_허용한다() {
        MediaUploadStartRequest request = request(MediaPurpose.FEED_CONTENT, 1L);

        assertThat(validator.validate(request)).isEmpty();
    }

    private MediaUploadStartRequest request(MediaPurpose purpose, Long targetId) {
        return new MediaUploadStartRequest(
                purpose,
                targetId,
                "image.webp",
                "image/webp",
                1024L
        );
    }
}

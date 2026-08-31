package com.shoutoutz.api.media.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class MediaPolicyTest {

    private final MediaUploadPolicy policy = new DefaultMediaUploadPolicy();

    @Test
    void 이미지_허용_ContentType과_크기를_통과시킨다() {
        policy.validateImage(" IMAGE/WEBP ", policy.maxImageSizeBytes());

        assertThat(policy.allowedImageContentTypes())
                .containsExactlyInAnyOrder("image/jpeg", "image/png", "image/webp");
    }

    @Test
    void 이미지가_아닌_ContentType을_거부한다() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> policy.validateImage("application/pdf", 1024));
    }

    @Test
    void 최대_크기를_초과한_이미지를_거부한다() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> policy.validateImage(
                        "image/jpeg", policy.maxImageSizeBytes() + 1));
    }

    @Test
    void 업로드_상태의_허용_전이를_검증한다() {
        assertThat(MediaStatus.PENDING_UPLOAD.canTransitionTo(MediaStatus.PROCESSING)).isTrue();
        assertThat(MediaStatus.PENDING_UPLOAD.canTransitionTo(MediaStatus.EXPIRED)).isTrue();
        assertThat(MediaStatus.PENDING_UPLOAD.canTransitionTo(MediaStatus.READY)).isFalse();
        assertThat(MediaStatus.PROCESSING.canTransitionTo(MediaStatus.READY)).isTrue();
    }
}

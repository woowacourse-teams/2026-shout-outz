package com.shoutoutz.api.media.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class MediaMetadataTest {

    private static final Instant NOW = Instant.parse("2026-08-31T00:00:00Z");

    @Test
    void 업로드_대기_메타데이터를_생성한다() {
        MediaMetadata metadata = MediaMetadata.initialize(
                MediaPurpose.PROJECT_DESCRIPTION,
                1L,
                "media/user-profile/1/original.webp",
                "사용자_이미지.webp ",
                "IMAGE/WEBP",
                1024,
                NOW.plus(5, ChronoUnit.MINUTES),
                NOW
        );

        assertThat(metadata.getStatus()).isEqualTo(MediaStatus.PENDING_UPLOAD);
        assertThat(metadata.getId()).isNull();
        assertThat(metadata.getUploadedBy()).isEqualTo(1L);
        assertThat(metadata.getOriginalFileName()).isEqualTo("사용자_이미지.webp");
        assertThat(metadata.getMimeType()).isEqualTo("image/webp");
        assertThat(metadata.getFailureReason()).isNull();
    }

    @Test
    void 업로드_확인_후_처리중으로_전환한다() {
        MediaMetadata metadata = initialize();

        MediaMetadata processing = metadata.confirmUpload(2048, NOW.plus(1, ChronoUnit.MINUTES));

        assertThat(processing.getStatus()).isEqualTo(MediaStatus.PROCESSING);
        assertThat(processing.getSizeBytes()).isEqualTo(2048);
        assertThat(processing.getUploadedAt()).isEqualTo(NOW.plus(1, ChronoUnit.MINUTES));
    }

    @Test
    void 처리_실패_시_실패_사유를_보존한다() {
        MediaMetadata metadata = initialize();

        MediaMetadata failed = metadata.markFailed("파일 시그니처 검증 실패", NOW.plus(1, ChronoUnit.MINUTES));

        assertThat(failed.getStatus()).isEqualTo(MediaStatus.FAILED);
        assertThat(failed.getFailureReason()).isEqualTo("파일 시그니처 검증 실패");
    }

    @Test
    void 실패_상태는_실패_사유가_필수다() {
        MediaMetadata metadata = initialize();

        assertThatThrownBy(() -> metadata.markFailed(" ", NOW.plus(1, ChronoUnit.MINUTES)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("실패 사유는 1자 이상 1,000자 이하여야 합니다.");
    }

    private MediaMetadata initialize() {
        return MediaMetadata.initialize(
                MediaPurpose.PROJECT_THUMBNAIL,
                1L,
                "media/project/1/thumbnail.webp",
                "thumbnail.webp",
                "image/webp",
                1024,
                NOW.plus(5, ChronoUnit.MINUTES),
                NOW
        );
    }
}

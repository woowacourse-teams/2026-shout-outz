package com.shoutoutz.api.media.infrastructure.mappper;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.infrastructure.MediaMetadataEntity;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MediaMetadataMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-31T00:00:00Z");
    private static final Instant UPLOADED_AT = Instant.parse("2026-08-31T00:01:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-08-31T00:02:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-08-31T00:05:00Z");

    @Test
    void 미디어_메타데이터의_필드를_엔티티와_도메인_사이에_그대로_변환한다() {
        MediaMetadata metadata = MediaMetadata.reconstitute(
                10L,
                7L,
                MediaPurpose.POST_CONTENT,
                "media/post-content/object-id",
                "post-image.webp",
                "image/webp",
                1024L,
                MediaStatus.FAILED,
                EXPIRES_AT,
                "파일 시그니처 검증 실패",
                UPLOADED_AT,
                CREATED_AT,
                UPDATED_AT
        );

        MediaMetadataEntity entity = MediaMetadataMapper.toEntity(metadata);

        assertThat(entity.getId()).isEqualTo(10L);
        assertThat(entity.getUploadedBy()).isEqualTo(7L);
        assertThat(entity.getPurpose()).isEqualTo(MediaPurpose.POST_CONTENT);
        assertThat(entity.getS3Key()).isEqualTo("media/post-content/object-id");
        assertThat(entity.getOriginalFileName()).isEqualTo("post-image.webp");
        assertThat(entity.getMimeType()).isEqualTo("image/webp");
        assertThat(entity.getSizeBytes()).isEqualTo(1024L);
        assertThat(entity.getStatus()).isEqualTo(MediaStatus.FAILED);
        assertThat(entity.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(entity.getFailureReason()).isEqualTo("파일 시그니처 검증 실패");
        assertThat(entity.getUploadedAt()).isEqualTo(UPLOADED_AT);
        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED_AT);

        MediaMetadata restored = MediaMetadataMapper.toDomain(entity);

        assertThat(restored.getId()).isEqualTo(metadata.getId());
        assertThat(restored.getUploadedBy()).isEqualTo(metadata.getUploadedBy());
        assertThat(restored.getPurpose()).isEqualTo(metadata.getPurpose());
        assertThat(restored.getS3Key()).isEqualTo(metadata.getS3Key());
        assertThat(restored.getOriginalFileName()).isEqualTo(metadata.getOriginalFileName());
        assertThat(restored.getMimeType()).isEqualTo(metadata.getMimeType());
        assertThat(restored.getSizeBytes()).isEqualTo(metadata.getSizeBytes());
        assertThat(restored.getStatus()).isEqualTo(metadata.getStatus());
        assertThat(restored.getExpiresAt()).isEqualTo(metadata.getExpiresAt());
        assertThat(restored.getFailureReason()).isEqualTo(metadata.getFailureReason());
        assertThat(restored.getUploadedAt()).isEqualTo(metadata.getUploadedAt());
        assertThat(restored.getCreatedAt()).isEqualTo(metadata.getCreatedAt());
        assertThat(restored.getUpdatedAt()).isEqualTo(metadata.getUpdatedAt());
    }
}

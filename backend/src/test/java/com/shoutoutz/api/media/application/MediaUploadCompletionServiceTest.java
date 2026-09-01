package com.shoutoutz.api.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.infrastructure.s3.S3MediaStorage;
import com.shoutoutz.api.media.infrastructure.s3.StoredMediaObject;
import com.shoutoutz.api.media.infrastructure.s3.exception.S3ObjectNotFoundException;
import com.shoutoutz.api.media.infrastructure.s3.exception.S3ObjectValidationException;
import com.shoutoutz.api.media.presentation.dto.response.MediaUploadCompleteResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaUploadCompletionServiceTest {

    private static final Instant NOW = Instant.parse("2099-09-01T00:00:00Z");

    @Mock
    private MediaMetadataRepository mediaMetadataRepository;

    @Mock
    private S3MediaStorage s3MediaStorage;

    private MediaUploadCompletionService mediaUploadCompletionService;

    @BeforeEach
    void setUp() {
        mediaUploadCompletionService = new MediaUploadCompletionService(
                mediaMetadataRepository,
                s3MediaStorage
        );
    }

    @Test
    void S3_객체를_검증하고_PROCESSING으로_전환한다() {
        MediaMetadata pending = pendingMetadata();
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.of(pending));
        when(s3MediaStorage.verifyUploadedObject(pending)).thenReturn(
                new StoredMediaObject(pending.getS3Key(), 1024L, "image/webp", "etag", NOW)
        );
        when(mediaMetadataRepository.save(any(MediaMetadata.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MediaUploadCompleteResponse response = mediaUploadCompletionService.completeUpload(7L, 10L);

        assertThat(response.mediaId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(MediaStatus.PROCESSING);
        assertThat(response.sizeBytes()).isEqualTo(1024L);
        assertThat(response.contentType()).isEqualTo("image/webp");
        assertThat(response.uploadedAt()).isNotNull();

        ArgumentCaptor<MediaMetadata> captor = ArgumentCaptor.forClass(MediaMetadata.class);
        verify(mediaMetadataRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MediaStatus.PROCESSING);
        verify(s3MediaStorage).verifyUploadedObject(pending);
    }

    @Test
    void S3_객체가_없으면_아직_업로드되지_않은_것으로_처리한다() {
        MediaMetadata pending = pendingMetadata();
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.of(pending));
        when(s3MediaStorage.verifyUploadedObject(pending)).thenThrow(
                new S3ObjectNotFoundException("not found", null)
        );

        assertThatThrownBy(() -> mediaUploadCompletionService.completeUpload(7L, 10L))
                .isInstanceOf(MediaUploadCompletionConflictException.class)
                .hasMessage("S3 객체가 아직 업로드되지 않았습니다.");

        verify(mediaMetadataRepository, never()).save(any(MediaMetadata.class));
    }

    @Test
    void S3_메타데이터가_다르면_FAILED로_기록한다() {
        MediaMetadata pending = pendingMetadata();
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.of(pending));
        when(s3MediaStorage.verifyUploadedObject(pending)).thenThrow(
                new S3ObjectValidationException("size mismatch")
        );
        when(mediaMetadataRepository.save(any(MediaMetadata.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> mediaUploadCompletionService.completeUpload(7L, 10L))
                .isInstanceOf(MediaUploadCompletionValidationException.class)
                .hasMessage("S3 객체 검증에 실패했습니다.");

        ArgumentCaptor<MediaMetadata> captor = ArgumentCaptor.forClass(MediaMetadata.class);
        verify(mediaMetadataRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MediaStatus.FAILED);
        assertThat(captor.getValue().getFailureReason()).isEqualTo("S3 객체 메타데이터 검증 실패");
    }

    @Test
    void 업로더가_아니면_완료할_수_없다() {
        MediaMetadata pending = pendingMetadata();
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> mediaUploadCompletionService.completeUpload(8L, 10L))
                .isInstanceOf(MediaUploadForbiddenException.class);

        verify(s3MediaStorage, never()).verifyUploadedObject(any(MediaMetadata.class));
    }

    @Test
    void PENDING_UPLOAD가_아닌_상태는_다시_완료하지_않는다() {
        MediaMetadata processing = MediaMetadata.reconstitute(
                10L,
                7L,
                MediaPurpose.POST_CONTENT,
                "media/post-content/object-id",
                "post-image.webp",
                "image/webp",
                1024L,
                MediaStatus.PROCESSING,
                NOW.plus(5, ChronoUnit.MINUTES),
                null,
                NOW,
                NOW,
                NOW
        );
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.of(processing));

        assertThatThrownBy(() -> mediaUploadCompletionService.completeUpload(7L, 10L))
                .isInstanceOf(MediaUploadCompletionConflictException.class);

        verify(s3MediaStorage, never()).verifyUploadedObject(any(MediaMetadata.class));
        verify(mediaMetadataRepository, never()).save(any(MediaMetadata.class));
    }

    @Test
    void 존재하지_않는_미디어는_찾을_수_없다() {
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaUploadCompletionService.completeUpload(7L, 10L))
                .isInstanceOf(MediaUploadNotFoundException.class);
    }

    private MediaMetadata pendingMetadata() {
        return MediaMetadata.reconstitute(
                10L,
                7L,
                MediaPurpose.POST_CONTENT,
                "media/post-content/object-id",
                "post-image.webp",
                "image/webp",
                1024L,
                MediaStatus.PENDING_UPLOAD,
                NOW.plus(5, ChronoUnit.MINUTES),
                null,
                null,
                NOW,
                NOW
        );
    }
}

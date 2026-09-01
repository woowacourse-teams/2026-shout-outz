package com.shoutoutz.api.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
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
class MediaImageProcessingWorkerTest {

    private static final Instant NOW = Instant.parse("2026-08-31T00:00:00Z");

    @Mock
    private MediaMetadataRepository mediaMetadataRepository;

    @Mock
    private MediaImageProcessor mediaImageProcessor;

    private MediaImageProcessingWorker worker;

    @BeforeEach
    void setUp() {
        worker = new MediaImageProcessingWorker(
                mediaMetadataRepository,
                mediaImageProcessor
        );
    }

    @Test
    void 이미지_처리가_성공하면_READY로_전환한다() {
        MediaMetadata processing = processingMetadata();
        when(mediaImageProcessor.process(processing)).thenReturn(
                new MediaImageProcessingResult(900L)
        );
        when(mediaMetadataRepository.save(any(MediaMetadata.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        worker.process(processing);

        verify(mediaImageProcessor).process(processing);
        ArgumentCaptor<MediaMetadata> captor = ArgumentCaptor.forClass(MediaMetadata.class);
        verify(mediaMetadataRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MediaStatus.READY);
        assertThat(captor.getValue().getSizeBytes()).isEqualTo(900L);
        assertThat(captor.getValue().getFailureReason()).isNull();
    }

    @Test
    void 이미지_처리가_실패하면_FAILED와_안전한_실패사유를_저장한다() {
        MediaMetadata processing = processingMetadata();
        when(mediaImageProcessor.process(processing)).thenThrow(
                new ImageProcessingException("파일 시그니처와 MIME 타입이 일치하지 않습니다.")
        );
        when(mediaMetadataRepository.save(any(MediaMetadata.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        worker.process(processing);

        ArgumentCaptor<MediaMetadata> captor = ArgumentCaptor.forClass(MediaMetadata.class);
        verify(mediaMetadataRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MediaStatus.FAILED);
        assertThat(captor.getValue().getFailureReason())
                .isEqualTo("파일 시그니처와 MIME 타입이 일치하지 않습니다.");
    }

    @Test
    void PROCESSING이_아닌_미디어는_다시_처리하지_않는다() {
        MediaMetadata ready = MediaMetadata.reconstitute(
                10L,
                7L,
                MediaPurpose.POST_CONTENT,
                "media/post-content/object-id",
                "post-image.png",
                "image/png",
                1024L,
                MediaStatus.READY,
                NOW.plus(5, ChronoUnit.MINUTES),
                null,
                NOW,
                NOW,
                NOW
        );

        worker.process(ready);

        verify(mediaImageProcessor, never()).process(any(MediaMetadata.class));
        verify(mediaMetadataRepository, never()).save(any(MediaMetadata.class));
    }

    private MediaMetadata processingMetadata() {
        return MediaMetadata.reconstitute(
                10L,
                7L,
                MediaPurpose.POST_CONTENT,
                "media/post-content/object-id",
                "post-image.png",
                "image/png",
                1024L,
                MediaStatus.PROCESSING,
                NOW.plus(5, ChronoUnit.MINUTES),
                null,
                NOW,
                NOW,
                NOW
        );
    }
}

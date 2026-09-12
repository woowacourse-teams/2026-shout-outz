package com.shoutoutz.api.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.media.application.exception.MediaAccessForbiddenException;
import com.shoutoutz.api.media.application.exception.MediaNotReadyException;
import com.shoutoutz.api.media.application.exception.MediaQueryNotFoundException;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.infrastructure.s3.MediaObjectKeyGenerator;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import com.shoutoutz.api.media.infrastructure.s3.PresignedDownload;
import com.shoutoutz.api.media.infrastructure.s3.S3MediaStorage;
import com.shoutoutz.api.media.presentation.dto.response.MediaDownloadResponse;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaQueryServiceTest {

    private static final Instant NOW = Instant.parse("2099-09-01T00:00:00Z");

    @Mock
    private MediaMetadataRepository mediaMetadataRepository;

    @Mock
    private MediaAccessAuthorizer mediaAccessAuthorizer;

    @Mock
    private MediaObjectKeyGenerator mediaObjectKeyGenerator;

    @Mock
    private S3MediaStorage s3MediaStorage;

    private MediaQueryService mediaQueryService;

    @BeforeEach
    void setUp() {
        mediaQueryService = new MediaQueryService(
                mediaMetadataRepository,
                mediaAccessAuthorizer,
                mediaObjectKeyGenerator,
                s3MediaStorage
        );
    }

    @Test
    void READY_미디어의_표시용_Presigned_GET_URL을_발급한다() {
        MediaMetadata metadata = readyMetadata();
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.of(metadata));
        when(mediaObjectKeyGenerator.generateVariant(metadata.getS3Key(), MediaVariant.DISPLAY))
                .thenReturn(metadata.getS3Key() + "/display");
        when(s3MediaStorage.createPresignedDownload(metadata.getS3Key() + "/display"))
                .thenReturn(new PresignedDownload(
                        metadata.getS3Key() + "/display",
                        URI.create("https://s3.example.com/display"),
                        NOW.plus(5, ChronoUnit.MINUTES)
                ));

        MediaDownloadResponse response = mediaQueryService.createDownloadUrl(
                7L,
                10L,
                MediaVariant.DISPLAY
        );

        assertThat(response.mediaId()).isEqualTo(10L);
        assertThat(response.variant()).isEqualTo(MediaVariant.DISPLAY);
        assertThat(response.downloadUrl()).hasToString("https://s3.example.com/display");
        assertThat(response.expiresAt()).isEqualTo(NOW.plus(5, ChronoUnit.MINUTES));
        assertThat(response.contentType()).isEqualTo("image/webp");
        verify(mediaAccessAuthorizer).authorize(7L, metadata);
    }

    @Test
    void 변형본을_지정하지_않으면_DISPLAY를_사용한다() {
        MediaMetadata metadata = readyMetadata();
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.of(metadata));
        when(mediaObjectKeyGenerator.generateVariant(metadata.getS3Key(), MediaVariant.DISPLAY))
                .thenReturn(metadata.getS3Key() + "/display");
        when(s3MediaStorage.createPresignedDownload(any(String.class))).thenReturn(
                new PresignedDownload(
                        metadata.getS3Key() + "/display",
                        URI.create("https://s3.example.com/display"),
                        NOW.plus(5, ChronoUnit.MINUTES)
                )
        );

        MediaDownloadResponse response = mediaQueryService.createDownloadUrl(7L, 10L, null);

        assertThat(response.variant()).isEqualTo(MediaVariant.DISPLAY);
        verify(mediaObjectKeyGenerator).generateVariant(metadata.getS3Key(), MediaVariant.DISPLAY);
    }

    @Test
    void 권한이_없으면_S3_URL을_발급하지_않는다() {
        MediaMetadata metadata = readyMetadata();
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.of(metadata));
        doThrow(new MediaAccessForbiddenException("미디어 조회 권한이 없습니다."))
                .when(mediaAccessAuthorizer).authorize(8L, metadata);

        assertThatThrownBy(() -> mediaQueryService.createDownloadUrl(8L, 10L, MediaVariant.DISPLAY))
                .isInstanceOf(MediaAccessForbiddenException.class);

        verify(s3MediaStorage, never()).createPresignedDownload(any(String.class));
    }

    @Test
    void READY가_아닌_미디어는_URL을_발급하지_않는다() {
        MediaMetadata metadata = MediaMetadata.reconstitute(
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
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.of(metadata));

        assertThatThrownBy(() -> mediaQueryService.createDownloadUrl(7L, 10L, MediaVariant.DISPLAY))
                .isInstanceOf(MediaNotReadyException.class);

        verify(s3MediaStorage, never()).createPresignedDownload(any(String.class));
    }

    @Test
    void 미디어가_없으면_404_예외를_던진다() {
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaQueryService.createDownloadUrl(7L, 10L, MediaVariant.DISPLAY))
                .isInstanceOf(MediaQueryNotFoundException.class);
    }

    private MediaMetadata readyMetadata() {
        return MediaMetadata.reconstitute(
                10L,
                7L,
                MediaPurpose.POST_CONTENT,
                "media/post-content/object-id",
                "post-image.webp",
                "image/webp",
                1024L,
                MediaStatus.READY,
                NOW.plus(5, ChronoUnit.MINUTES),
                null,
                NOW,
                NOW,
                NOW
        );
    }
}

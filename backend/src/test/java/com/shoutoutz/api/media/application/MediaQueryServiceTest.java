package com.shoutoutz.api.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.media.application.exception.MediaNotReadyException;
import com.shoutoutz.api.media.application.exception.MediaQueryNotFoundException;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.infrastructure.s3.MediaObjectKeyGenerator;
import com.shoutoutz.api.media.infrastructure.s3.MediaPublicUrlResolver;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import com.shoutoutz.api.media.presentation.dto.response.MediaDownloadResponse;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaQueryServiceTest {

    private static final URI PUBLIC_URL = URI.create("https://cdn.example.com/display");
    private static final Instant NOW = Instant.parse("2099-09-01T00:00:00Z");

    @Mock
    private MediaMetadataRepository mediaMetadataRepository;

    @Mock
    private MediaObjectKeyGenerator mediaObjectKeyGenerator;

    @Mock
    private MediaPublicUrlResolver mediaPublicUrlResolver;

    private MediaQueryService mediaQueryService;

    @BeforeEach
    void setUp() {
        mediaQueryService = new MediaQueryService(
                mediaMetadataRepository,
                mediaObjectKeyGenerator,
                mediaPublicUrlResolver
        );
    }

    @Test
    void READY_미디어의_CloudFront_공개_URL을_반환한다() {
        MediaMetadata metadata = readyMetadata();
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.of(metadata));
        when(mediaObjectKeyGenerator.generateVariant(metadata.getS3Key(), MediaVariant.DISPLAY))
                .thenReturn(metadata.getS3Key() + "/display");
        when(mediaPublicUrlResolver.resolve(metadata.getS3Key() + "/display"))
                .thenReturn(PUBLIC_URL);

        MediaDownloadResponse response = mediaQueryService.resolvePublicUrl(
                10L,
                MediaVariant.DISPLAY
        );

        assertThat(response.variant()).isEqualTo(MediaVariant.DISPLAY);
        assertThat(response.url()).isEqualTo(PUBLIC_URL);
        assertThat(response.contentType()).isEqualTo("image/webp");
        verify(mediaPublicUrlResolver).resolve(metadata.getS3Key() + "/display");
    }

    @Test
    void 변형본을_지정하지_않으면_DISPLAY를_사용한다() {
        MediaMetadata metadata = readyMetadata();
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.of(metadata));
        when(mediaObjectKeyGenerator.generateVariant(metadata.getS3Key(), MediaVariant.DISPLAY))
                .thenReturn(metadata.getS3Key() + "/display");
        when(mediaPublicUrlResolver.resolve(metadata.getS3Key() + "/display"))
                .thenReturn(PUBLIC_URL);

        MediaDownloadResponse response = mediaQueryService.resolvePublicUrl(10L, null);

        assertThat(response.variant()).isEqualTo(MediaVariant.DISPLAY);
        verify(mediaObjectKeyGenerator).generateVariant(metadata.getS3Key(), MediaVariant.DISPLAY);
    }

    @Test
    void READY가_아닌_미디어는_URL을_반환하지_않는다() {
        MediaMetadata metadata = MediaMetadata.reconstitute(
                10L,
                7L,
                MediaPurpose.FEED_CONTENT,
                "media/feed-content/object-id",
                "feed-image.webp",
                "image/webp",
                1024L,
                MediaStatus.PROCESSING,
                NOW,
                null,
                NOW,
                NOW,
                NOW
        );
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.of(metadata));

        assertThatThrownBy(() -> mediaQueryService.resolvePublicUrl(10L, MediaVariant.DISPLAY))
                .isInstanceOf(MediaNotReadyException.class);

        verify(mediaPublicUrlResolver, never()).resolve(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void 미디어가_없으면_404_예외를_던진다() {
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaQueryService.resolvePublicUrl(10L, MediaVariant.DISPLAY))
                .isInstanceOf(MediaQueryNotFoundException.class);
    }

    private MediaMetadata readyMetadata() {
        return MediaMetadata.reconstitute(
                10L,
                7L,
                MediaPurpose.FEED_CONTENT,
                "media/feed-content/object-id",
                "feed-image.webp",
                "image/webp",
                1024L,
                MediaStatus.READY,
                NOW,
                null,
                NOW,
                NOW,
                NOW
        );
    }
}

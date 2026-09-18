package com.shoutoutz.api.homebanner.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.media.application.MediaUrlResolver;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
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
class HomeBannerImageServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-17T00:00:00Z");

    @Mock
    private MediaMetadataRepository mediaMetadataRepository;

    @Mock
    private MediaUrlResolver mediaUrlResolver;

    private HomeBannerImageService imageService;

    @BeforeEach
    void setUp() {
        imageService = new HomeBannerImageService(mediaMetadataRepository, mediaUrlResolver);
    }

    @Test
    void READY_HOME_BANNER_미디어의_표시_URL을_반환한다() {
        MediaMetadata media = metadata(MediaPurpose.HOME_BANNER, MediaStatus.READY);
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.of(media));
        when(mediaUrlResolver.resolve(media, MediaVariant.DISPLAY))
                .thenReturn(URI.create("https://cdn.example.com/banner"));

        URI imageUrl = imageService.createImageUrl(10L);

        assertThat(imageUrl).hasToString("https://cdn.example.com/banner");
    }

    @Test
    void 미디어가_없으면_404_예외를_던진다() {
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> imageService.createImageUrl(10L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void 다른_목적의_미디어는_사용할_수_없다() {
        when(mediaMetadataRepository.findById(10L))
                .thenReturn(Optional.of(metadata(MediaPurpose.FEED_CONTENT, MediaStatus.READY)));

        assertThatThrownBy(() -> imageService.createImageUrl(10L))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void READY가_아닌_미디어는_사용할_수_없다() {
        when(mediaMetadataRepository.findById(10L))
                .thenReturn(Optional.of(metadata(MediaPurpose.HOME_BANNER, MediaStatus.PROCESSING)));

        assertThatThrownBy(() -> imageService.createImageUrl(10L))
                .isInstanceOf(ConflictException.class);
    }

    private MediaMetadata metadata(MediaPurpose purpose, MediaStatus status) {
        return MediaMetadata.reconstitute(
                10L,
                1L,
                purpose,
                "media/home-banner/object-id",
                "banner.webp",
                "image/webp",
                1024L,
                status,
                NOW.plus(5, ChronoUnit.MINUTES),
                null,
                NOW,
                NOW,
                NOW
        );
    }
}

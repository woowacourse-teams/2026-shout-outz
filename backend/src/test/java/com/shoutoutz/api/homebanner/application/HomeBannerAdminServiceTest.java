package com.shoutoutz.api.homebanner.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.homebanner.domain.BannerDestinationType;
import com.shoutoutz.api.homebanner.domain.BannerTargetType;
import com.shoutoutz.api.homebanner.domain.HomeBanner;
import com.shoutoutz.api.homebanner.domain.HomeBannerRepository;
import com.shoutoutz.api.homebanner.presentation.dto.request.HomeBannerUpsertRequest;
import com.shoutoutz.api.media.application.MediaQueryService;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import com.shoutoutz.api.media.presentation.dto.response.MediaDownloadResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
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
class HomeBannerAdminServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-17T00:00:00Z");

    @Mock
    private HomeBannerRepository homeBannerRepository;

    @Mock
    private MediaMetadataRepository mediaMetadataRepository;

    @Mock
    private MediaQueryService mediaQueryService;

    @Mock
    private HomeBannerTargetValidator targetValidator;

    private HomeBannerAdminService service;

    @BeforeEach
    void setUp() {
        service = new HomeBannerAdminService(
                homeBannerRepository,
                mediaMetadataRepository,
                mediaQueryService,
                targetValidator
        );
    }

    @Test
    void 관리자가_배너를_등록한다() {
        HomeBannerUpsertRequest request = request();
        MediaMetadata media = readyBannerMedia();
        HomeBanner saved = savedBanner();
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.of(media));
        when(homeBannerRepository.save(any(HomeBanner.class))).thenReturn(saved);
        when(mediaQueryService.createDownloadUrl(media, MediaVariant.DISPLAY))
                .thenReturn(downloadResponse());

        var response = service.save(1L, UserRole.ADMIN, request);

        assertThat(response.bannerId()).isEqualTo(100L);
        assertThat(response.imageUrl()).hasToString("https://s3.example.com/banner");
        verify(targetValidator).validate(BannerTargetType.PROJECT, 20L);
    }

    @Test
    void 일반_사용자는_관리_API를_사용할_수_없다() {
        assertThatThrownBy(() -> service.save(1L, UserRole.USER, request()))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(homeBannerRepository, mediaMetadataRepository, mediaQueryService);
    }

    @Test
    void READY_HOME_BANNER가_아닌_미디어는_사용할_수_없다() {
        MediaMetadata media = metadata(MediaPurpose.FEED_CONTENT, MediaStatus.READY);
        when(mediaMetadataRepository.findById(10L)).thenReturn(Optional.of(media));

        assertThatThrownBy(() -> service.save(1L, UserRole.ADMIN, request()))
                .isInstanceOf(ConflictException.class);

        verifyNoInteractions(homeBannerRepository);
    }

    @Test
    void 없는_배너는_수정할_수_없다() {
        when(homeBannerRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(100L, UserRole.ADMIN, request()))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(mediaMetadataRepository);
    }

    @Test
    void 없는_배너는_삭제할_수_없다() {
        when(homeBannerRepository.deleteById(100L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(100L, UserRole.ADMIN))
                .isInstanceOf(NotFoundException.class);
    }

    private HomeBannerUpsertRequest request() {
        return new HomeBannerUpsertRequest(
                10L,
                BannerDestinationType.TARGET,
                BannerTargetType.PROJECT,
                20L,
                null,
                null,
                0,
                true
        );
    }

    private HomeBanner savedBanner() {
        return HomeBanner.reconstitute(
                100L,
                10L,
                BannerDestinationType.TARGET,
                BannerTargetType.PROJECT,
                20L,
                null,
                null,
                0,
                true,
                1L,
                NOW,
                NOW
        );
    }

    private MediaMetadata readyBannerMedia() {
        return metadata(MediaPurpose.HOME_BANNER, MediaStatus.READY);
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

    private MediaDownloadResponse downloadResponse() {
        return new MediaDownloadResponse(
                10L,
                MediaVariant.DISPLAY,
                URI.create("https://s3.example.com/banner"),
                NOW.plus(5, ChronoUnit.MINUTES),
                "image/webp"
        );
    }
}

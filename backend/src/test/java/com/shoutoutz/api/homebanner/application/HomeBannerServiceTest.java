package com.shoutoutz.api.homebanner.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.homebanner.domain.BannerDestinationType;
import com.shoutoutz.api.homebanner.domain.BannerTargetType;
import com.shoutoutz.api.homebanner.domain.HomeBanner;
import com.shoutoutz.api.homebanner.domain.HomeBannerRepository;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeBannerServiceTest {

    @Mock
    private HomeBannerRepository homeBannerRepository;

    @Mock
    private HomeBannerImageService imageService;

    private HomeBannerService service;

    @BeforeEach
    void setUp() {
        service = new HomeBannerService(homeBannerRepository, imageService);
    }

    @Test
    void 활성_배너를_전체_조회한다() {
        HomeBanner banner = banner();
        when(homeBannerRepository.findAllActive()).thenReturn(List.of(banner));
        when(imageService.createImageUrl(10L))
                .thenReturn(URI.create("https://s3.example.com/banner"));

        var response = service.findAll();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().bannerId()).isEqualTo(100L);
        assertThat(response.getFirst().imageUrl()).hasToString("https://s3.example.com/banner");
        verify(homeBannerRepository).findAllActive();
    }

    private HomeBanner banner() {
        Instant now = Instant.parse("2026-09-17T00:00:00Z");
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
                now,
                now
        );
    }
}

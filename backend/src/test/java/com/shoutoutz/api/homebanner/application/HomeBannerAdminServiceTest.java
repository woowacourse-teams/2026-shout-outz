package com.shoutoutz.api.homebanner.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.homebanner.domain.BannerDestinationType;
import com.shoutoutz.api.homebanner.domain.BannerTargetType;
import com.shoutoutz.api.homebanner.domain.HomeBanner;
import com.shoutoutz.api.homebanner.domain.HomeBannerRepository;
import com.shoutoutz.api.homebanner.presentation.dto.request.HomeBannerUpsertRequest;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.net.URI;
import java.time.Instant;
import java.util.List;
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
    private HomeBannerImageService imageService;

    @Mock
    private HomeBannerTargetValidator targetValidator;

    private HomeBannerAdminService service;

    @BeforeEach
    void setUp() {
        service = new HomeBannerAdminService(
                homeBannerRepository,
                imageService,
                targetValidator
        );
    }

    @Test
    void 관리자가_배너를_등록한다() {
        HomeBannerUpsertRequest request = request();
        HomeBanner saved = savedBanner();
        when(homeBannerRepository.save(any(HomeBanner.class))).thenReturn(saved);
        when(imageService.createImageUrl(10L))
                .thenReturn(URI.create("https://cdn.example.com/banner"));

        var response = service.save(1L, UserRole.ADMIN, request);

        assertThat(response.bannerId()).isEqualTo(100L);
        assertThat(response.imageUrl()).hasToString("https://cdn.example.com/banner");
        verify(targetValidator).validate(BannerTargetType.PROJECT, 20L);
    }

    @Test
    void 관리자가_전체_배너를_조회한다() {
        HomeBanner banner = savedBanner();
        when(homeBannerRepository.findAll()).thenReturn(List.of(banner));
        when(imageService.createImageUrl(10L))
                .thenReturn(URI.create("https://cdn.example.com/banner"));

        var responses = service.findAll(UserRole.ADMIN);

        assertThat(responses).singleElement()
                .satisfies(response -> assertThat(response.bannerId()).isEqualTo(100L));
    }

    @Test
    void 관리자가_배너를_수정한다() {
        HomeBanner banner = savedBanner();
        when(homeBannerRepository.findById(100L)).thenReturn(Optional.of(banner));
        when(homeBannerRepository.update(any(HomeBanner.class))).thenReturn(Optional.of(banner));
        when(imageService.createImageUrl(10L))
                .thenReturn(URI.create("https://cdn.example.com/banner"));

        var response = service.update(100L, UserRole.ADMIN, request());

        assertThat(response.bannerId()).isEqualTo(100L);
        verify(targetValidator).validate(BannerTargetType.PROJECT, 20L);
    }

    @Test
    void 관리자가_배너를_삭제한다() {
        when(homeBannerRepository.deleteById(100L)).thenReturn(true);

        service.delete(100L, UserRole.ADMIN);

        verify(homeBannerRepository).deleteById(100L);
    }

    @Test
    void 일반_사용자는_관리_API를_사용할_수_없다() {
        assertThatThrownBy(() -> service.save(1L, UserRole.USER, request()))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(homeBannerRepository, imageService);
    }

    @Test
    void 없는_배너는_수정할_수_없다() {
        when(homeBannerRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(100L, UserRole.ADMIN, request()))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(imageService);
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

}

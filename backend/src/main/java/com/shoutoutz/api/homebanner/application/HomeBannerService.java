package com.shoutoutz.api.homebanner.application;

import com.shoutoutz.api.homebanner.domain.HomeBanner;
import com.shoutoutz.api.homebanner.domain.HomeBannerRepository;
import com.shoutoutz.api.homebanner.presentation.dto.response.HomeBannerResponse;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeBannerService {

    private final HomeBannerRepository homeBannerRepository;
    private final HomeBannerImageService imageService;

    @Transactional(readOnly = true)
    public List<HomeBannerResponse> findAll() {
        return homeBannerRepository.findAllActive().stream()
                .map(this::toResponse)
                .toList();
    }

    private HomeBannerResponse toResponse(HomeBanner banner) {
        URI imageUrl = imageService.createImageUrl(banner.getMediaId());
        return new HomeBannerResponse(
                banner.getId(),
                banner.getMediaId(),
                imageUrl,
                banner.getDestinationType().name(),
                enumName(banner.getTargetType()),
                banner.getTargetId(),
                enumName(banner.getLinkType()),
                banner.getLinkUrl()
        );
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}

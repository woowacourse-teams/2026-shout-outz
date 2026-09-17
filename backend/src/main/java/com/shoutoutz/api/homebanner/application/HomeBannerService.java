package com.shoutoutz.api.homebanner.application;

import com.shoutoutz.api.homebanner.domain.HomeBannerRepository;
import com.shoutoutz.api.homebanner.presentation.dto.response.HomeBannerResponse;
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
                .map(banner -> HomeBannerResponse.from(
                        banner,
                        imageService.createImageUrl(banner.getMediaId())
                ))
                .toList();
    }
}

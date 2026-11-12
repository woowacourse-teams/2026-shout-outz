package com.shoutoutz.api.homebanner.application;

import com.shoutoutz.api.common.cache.LocalCache;
import com.shoutoutz.api.common.cache.LocalCacheFactory;
import com.shoutoutz.api.homebanner.domain.BannerTargetType;
import com.shoutoutz.api.homebanner.domain.HomeBanner;
import com.shoutoutz.api.homebanner.domain.HomeBannerRepository;
import com.shoutoutz.api.homebanner.presentation.dto.response.HomeBannerResponse;
import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 공개 배너 목록은 모든 사용자에게 같으므로 로컬 캐시에 보관한다.
 * 관리자가 배너를 바꾸면 {@link #evictCache()}로 비운다.
 */
@Service
public class HomeBannerService {

    private static final String CACHE_NAME = "home-banners";
    private static final String CACHE_KEY = "active";

    private final HomeBannerRepository homeBannerRepository;
    private final HomeBannerImageService imageService;
    private final LocalCache<String, List<HomeBannerResponse>> cache;

    public HomeBannerService(
            HomeBannerRepository homeBannerRepository,
            HomeBannerImageService imageService,
            LocalCacheFactory cacheFactory
    ) {
        this.homeBannerRepository = homeBannerRepository;
        this.imageService = imageService;
        this.cache = cacheFactory.create(CACHE_NAME, key -> load());
    }

    public List<HomeBannerResponse> findAll() {
        return cache.get(CACHE_KEY);
    }

    /**
     * 배너를 바꾼 트랜잭션이 커밋된 뒤 보관한 목록을 지운다. 다음 조회가 새로 읽는다.
     */
    public void evictCache() {
        cache.evictAfterCommit(CACHE_KEY);
    }

    private List<HomeBannerResponse> load() {
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
                targetIdOf(banner),
                banner.getTargetSlug(),
                enumName(banner.getLinkType()),
                banner.getLinkUrl()
        );
    }

    /**
     * 프로젝트는 slug로 가리키므로 ID를 내려주지 않는다. 뉴스·피드는 slug가 없어 ID를 내려준다.
     */
    private Long targetIdOf(HomeBanner banner) {
        return banner.getTargetType() == BannerTargetType.PROJECT ? null : banner.getTargetId();
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}

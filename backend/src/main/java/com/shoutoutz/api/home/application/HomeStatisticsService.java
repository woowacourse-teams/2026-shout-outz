package com.shoutoutz.api.home.application;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.common.cache.LocalCache;
import com.shoutoutz.api.common.cache.LocalCacheFactory;
import com.shoutoutz.api.home.application.dto.HomeStatisticsCounts;
import com.shoutoutz.api.home.presentation.dto.response.HomeStatisticsResponse;
import java.time.Clock;
import org.springframework.stereotype.Service;

/**
 * 홈 통계는 모든 사용자에게 같으므로 로컬 캐시에 보관한다.
 * 숫자를 바꾸는 쓰기 경로가 많고 진행 중 이벤트 수는 시간만 지나도 바뀌어서, 비우지 않고 갱신 시간에 맡긴다.
 */
@Service
public class HomeStatisticsService {

    private static final String CACHE_NAME = "home-statistics";
    private static final String CACHE_KEY = "all";

    private final HomeStatisticsQueryRepository homeStatisticsQueryRepository;
    private final Clock clock;
    private final LocalCache<String, HomeStatisticsResponse> cache;

    public HomeStatisticsService(
            HomeStatisticsQueryRepository homeStatisticsQueryRepository,
            Clock clock,
            LocalCacheFactory cacheFactory
    ) {
        this.homeStatisticsQueryRepository = homeStatisticsQueryRepository;
        this.clock = clock;
        this.cache = cacheFactory.create(CACHE_NAME, key -> load());
    }

    public HomeStatisticsResponse find() {
        return cache.get(CACHE_KEY);
    }

    private HomeStatisticsResponse load() {
        HomeStatisticsCounts counts = homeStatisticsQueryRepository.find(clock.instant());
        return new HomeStatisticsResponse(
                counts.projectCount(),
                counts.feedCount(),
                // currentCohort는 최신 기수 번호가 아니라 현재 시스템에 정의된 전체 기수 개수다.
                Cohort.values().length,
                counts.ongoingEventCount()
        );
    }
}

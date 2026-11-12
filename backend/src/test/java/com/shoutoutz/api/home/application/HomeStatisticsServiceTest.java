package com.shoutoutz.api.home.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.shoutoutz.api.common.cache.LocalCacheFactory;
import com.shoutoutz.api.common.cache.TestLocalCacheFactory;
import com.shoutoutz.api.home.application.dto.HomeStatisticsCounts;
import com.shoutoutz.api.home.presentation.dto.response.HomeStatisticsResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeStatisticsServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");

    @Mock
    private HomeStatisticsQueryRepository homeStatisticsQueryRepository;

    @Test
    void returnsCountsAndTotalCohortCount() {
        given(homeStatisticsQueryRepository.find(NOW))
                .willReturn(new HomeStatisticsCounts(128L, 341L, 2L));
        HomeStatisticsService service = service(TestLocalCacheFactory.enabled("home-statistics"));

        HomeStatisticsResponse response = service.find();

        assertThat(response).isEqualTo(new HomeStatisticsResponse(128L, 341L, 8, 2L));
    }

    @Test
    @DisplayName("다시 조회하면 저장소를 다시 읽지 않고 보관한 통계를 돌려준다")
    void returnsCachedStatistics() {
        given(homeStatisticsQueryRepository.find(NOW))
                .willReturn(new HomeStatisticsCounts(128L, 341L, 2L));
        HomeStatisticsService service = service(TestLocalCacheFactory.enabled("home-statistics"));

        HomeStatisticsResponse first = service.find();
        HomeStatisticsResponse second = service.find();

        assertThat(second).isEqualTo(first);
        verify(homeStatisticsQueryRepository, times(1)).find(NOW);
    }

    @Test
    @DisplayName("캐시가 꺼져 있으면 조회할 때마다 저장소를 읽는다")
    void readsEveryTimeWhenCacheDisabled() {
        given(homeStatisticsQueryRepository.find(NOW))
                .willReturn(new HomeStatisticsCounts(128L, 341L, 2L));
        HomeStatisticsService service = service(TestLocalCacheFactory.disabled("home-statistics"));

        service.find();
        service.find();

        verify(homeStatisticsQueryRepository, times(2)).find(NOW);
    }

    private HomeStatisticsService service(LocalCacheFactory cacheFactory) {
        return new HomeStatisticsService(
                homeStatisticsQueryRepository,
                Clock.fixed(NOW, ZoneOffset.UTC),
                cacheFactory
        );
    }
}

package com.shoutoutz.api.home.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.shoutoutz.api.home.application.dto.HomeStatisticsCounts;
import com.shoutoutz.api.home.presentation.dto.response.HomeStatisticsResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
        HomeStatisticsService service = new HomeStatisticsService(
                homeStatisticsQueryRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        HomeStatisticsResponse response = service.find();

        assertThat(response).isEqualTo(new HomeStatisticsResponse(128L, 341L, 8, 2L));
    }
}

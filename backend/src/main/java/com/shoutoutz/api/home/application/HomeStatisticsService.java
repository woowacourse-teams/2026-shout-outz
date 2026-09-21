package com.shoutoutz.api.home.application;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.home.application.dto.HomeStatisticsCounts;
import com.shoutoutz.api.home.presentation.dto.response.HomeStatisticsResponse;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeStatisticsService {

    private final HomeStatisticsQueryRepository homeStatisticsQueryRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public HomeStatisticsResponse find() {
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

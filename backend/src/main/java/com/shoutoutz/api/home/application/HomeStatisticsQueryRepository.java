package com.shoutoutz.api.home.application;

import com.shoutoutz.api.home.application.dto.HomeStatisticsCounts;
import java.time.Instant;

/**
 * 홈 화면 통계 조회 포트.
 */
public interface HomeStatisticsQueryRepository {

    HomeStatisticsCounts find(Instant now);
}

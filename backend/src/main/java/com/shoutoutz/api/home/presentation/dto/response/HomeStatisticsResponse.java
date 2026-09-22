package com.shoutoutz.api.home.presentation.dto.response;

public record HomeStatisticsResponse(
        long projectCount,
        long feedCount,
        int currentCohort,
        long ongoingEventCount
) {
}

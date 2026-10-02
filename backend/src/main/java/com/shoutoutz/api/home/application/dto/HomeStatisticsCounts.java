package com.shoutoutz.api.home.application.dto;

public record HomeStatisticsCounts(
        long projectCount,
        long feedCount,
        long ongoingEventCount
) {
}

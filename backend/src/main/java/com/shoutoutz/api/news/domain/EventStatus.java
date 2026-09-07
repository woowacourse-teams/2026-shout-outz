package com.shoutoutz.api.news.domain;

import java.time.Instant;

public enum EventStatus {
    UPCOMING,
    ONGOING,
    ENDED;

    public static EventStatus from(Instant now, Instant startAt, Instant endAt) {
        if (now.isBefore(startAt)) {
            return UPCOMING;
        }
        if (now.isAfter(endAt)) {
            return ENDED;
        }
        return ONGOING;
    }
}

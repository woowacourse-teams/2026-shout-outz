package com.shoutoutz.api.news.domain.enums;

import com.shoutoutz.api.news.domain.NewsEventPeriod;
import java.time.Instant;

public enum EventStatus {
    UPCOMING,
    ONGOING,
    ENDED;

    public static EventStatus from(Instant now, Instant startAt, Instant endAt) {
        return new NewsEventPeriod(startAt, endAt).statusAt(now);
    }
}

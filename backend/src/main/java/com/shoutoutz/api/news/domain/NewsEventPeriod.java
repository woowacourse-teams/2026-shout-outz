package com.shoutoutz.api.news.domain;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.time.Instant;
import java.util.Objects;

import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_EVENT_END_AT_NULL;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_EVENT_PERIOD_INVALID;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_EVENT_START_AT_NULL;

/**
 * 이벤트가 진행되는 시간 구간을 표현하는 값 객체.
 *
 * <p>시작·종료 시각의 존재 여부와 순서는 이벤트 기간을 사용하는 모든 경로에서
 * 동일하게 검증되어야 하므로, 소식 aggregate의 공통 검증 코드와 분리한다.</p>
 */
public record NewsEventPeriod(Instant startAt, Instant endAt) {

    public NewsEventPeriod {
        if (startAt == null) {
            throw new DomainValidationException(NEWS_EVENT_START_AT_NULL);
        }
        if (endAt == null) {
            throw new DomainValidationException(NEWS_EVENT_END_AT_NULL);
        }
        if (startAt.isAfter(endAt)) {
            throw new DomainValidationException(NEWS_EVENT_PERIOD_INVALID);
        }
    }

    public EventStatus statusAt(Instant now) {
        Objects.requireNonNull(now, "now");
        if (now.isBefore(startAt)) {
            return EventStatus.UPCOMING;
        }
        if (now.isAfter(endAt)) {
            return EventStatus.ENDED;
        }
        return EventStatus.ONGOING;
    }
}

package com.shoutoutz.api.news.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NewsEventPeriodTest {

    private static final Instant START_AT = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant END_AT = Instant.parse("2026-09-30T23:59:59Z");

    @Test
    @DisplayName("이벤트 기간은 시작과 종료 시각을 보존한다")
    void preservesStartAndEndAt() {
        NewsEventPeriod period = new NewsEventPeriod(START_AT, END_AT);

        assertThat(period.startAt()).isEqualTo(START_AT);
        assertThat(period.endAt()).isEqualTo(END_AT);
    }

    @Test
    @DisplayName("이벤트 시작 시각이 null이면 생성할 수 없다")
    void rejectsNullStartAt() {
        assertThatThrownBy(() -> new NewsEventPeriod(null, END_AT))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(NewsErrorCode.NEWS_EVENT_START_AT_NULL));
    }

    @Test
    @DisplayName("이벤트 종료 시각이 null이면 생성할 수 없다")
    void rejectsNullEndAt() {
        assertThatThrownBy(() -> new NewsEventPeriod(START_AT, null))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(NewsErrorCode.NEWS_EVENT_END_AT_NULL));
    }

    @Test
    @DisplayName("시작 시각이 종료 시각보다 늦으면 생성할 수 없다")
    void rejectsReversedPeriod() {
        assertThatThrownBy(() -> new NewsEventPeriod(END_AT, START_AT))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(NewsErrorCode.NEWS_EVENT_PERIOD_INVALID));
    }

    @Test
    @DisplayName("시작과 종료 시각은 이벤트 진행 중 상태로 판정한다")
    void treatsPeriodBoundariesAsOngoing() {
        NewsEventPeriod period = new NewsEventPeriod(START_AT, END_AT);

        assertThat(period.statusAt(START_AT)).isEqualTo(EventStatus.ONGOING);
        assertThat(period.statusAt(END_AT)).isEqualTo(EventStatus.ONGOING);
    }

    @Test
    @DisplayName("이벤트 기간 전후 시각은 예정과 종료 상태로 판정한다")
    void determinesUpcomingAndEndedStatus() {
        NewsEventPeriod period = new NewsEventPeriod(START_AT, END_AT);

        assertThat(period.statusAt(START_AT.minusNanos(1))).isEqualTo(EventStatus.UPCOMING);
        assertThat(period.statusAt(END_AT.plusNanos(1))).isEqualTo(EventStatus.ENDED);
    }
}

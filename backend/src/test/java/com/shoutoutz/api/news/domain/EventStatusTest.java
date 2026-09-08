package com.shoutoutz.api.news.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventStatusTest {

    private static final Instant START_AT = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant END_AT = Instant.parse("2026-09-30T23:59:59Z");

    @Test
    @DisplayName("이벤트 시작 전이면 예정 상태를 반환한다")
    void returnsUpcomingBeforeStart() {
        assertThat(EventStatus.from(Instant.parse("2026-08-31T23:59:59Z"), START_AT, END_AT))
                .isEqualTo(EventStatus.UPCOMING);
    }

    @Test
    @DisplayName("이벤트 시작·종료 시각을 포함한 기간에는 진행 중 상태를 반환한다")
    void returnsOngoingDuringEventPeriod() {
        assertThat(EventStatus.from(START_AT, START_AT, END_AT)).isEqualTo(EventStatus.ONGOING);
        assertThat(EventStatus.from(Instant.parse("2026-09-15T00:00:00Z"), START_AT, END_AT))
                .isEqualTo(EventStatus.ONGOING);
        assertThat(EventStatus.from(END_AT, START_AT, END_AT)).isEqualTo(EventStatus.ONGOING);
    }

    @Test
    @DisplayName("이벤트 종료 후에는 종료 상태를 반환한다")
    void returnsEndedAfterEnd() {
        assertThat(EventStatus.from(Instant.parse("2026-10-01T00:00:00Z"), START_AT, END_AT))
                .isEqualTo(EventStatus.ENDED);
    }
}

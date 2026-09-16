package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestorableProjectTest {

    private static final Instant RESTORE_DEADLINE_AT = Instant.parse("2026-08-31T01:00:00Z");

    @Test
    @DisplayName("복구 기한 이전에는 복구할 수 있다.")
    void isRestorableBeforeDeadline() {
        assertThat(restorable().isRestorable(RESTORE_DEADLINE_AT.minusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("복구 기한과 같은 시각에는 복구할 수 있다.")
    void isRestorableAtDeadline() {
        assertThat(restorable().isRestorable(RESTORE_DEADLINE_AT)).isTrue();
    }

    @Test
    @DisplayName("복구 기한이 지나면 복구할 수 없다.")
    void isNotRestorableAfterDeadline() {
        assertThat(restorable().isRestorable(RESTORE_DEADLINE_AT.plusSeconds(1))).isFalse();
    }

    private static RestorableProject restorable() {
        return new RestorableProject(1L, RESTORE_DEADLINE_AT);
    }
}

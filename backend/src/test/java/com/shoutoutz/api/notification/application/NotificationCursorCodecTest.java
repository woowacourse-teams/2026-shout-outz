package com.shoutoutz.api.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.notification.application.dto.NotificationCursor;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NotificationCursorCodecTest {

    private final NotificationCursorCodec codec = new NotificationCursorCodec();

    @Test
    void 생성_시각과_ID를_커서로_변환하고_복원한다() {
        NotificationCursor expected = new NotificationCursor(
                Instant.parse("2026-09-30T00:00:00Z"),
                42L
        );

        NotificationCursor result = codec.decode(codec.encode(expected));

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void 잘못된_커서를_거부한다() {
        assertThatThrownBy(() -> codec.decode("invalid"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 음수_ID가_포함된_커서를_거부한다() {
        NotificationCursor invalid = new NotificationCursor(
                Instant.parse("2026-09-30T00:00:00Z"),
                -1L
        );

        assertThatThrownBy(() -> codec.decode(codec.encode(invalid)))
                .isInstanceOf(BadRequestException.class);
    }
}

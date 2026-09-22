package com.shoutoutz.api.feed.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.feed.application.dto.FeedCursor;
import com.shoutoutz.api.feed.application.dto.FeedSort;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FeedCursorCodecTest {

    private final FeedCursorCodec codec = new FeedCursorCodec();

    @Test
    void 좋아요_수와_생성_시각과_ID를_커서로_변환하고_복원한다() {
        Instant createdAt = Instant.parse("2026-09-11T00:00:00Z");
        FeedCursor expected = new FeedCursor(FeedSort.POPULAR, 0, 42L, createdAt, 10L);

        FeedCursor cursor = codec.decode(codec.encode(expected), FeedSort.POPULAR);

        assertThat(cursor).isEqualTo(expected);
    }

    @Test
    void 정확도_등급을_커서로_변환하고_복원한다() {
        Instant createdAt = Instant.parse("2026-09-11T00:00:00Z");
        FeedCursor expected = new FeedCursor(
                FeedSort.RELEVANCE,
                3,
                0L,
                createdAt,
                10L
        );

        FeedCursor cursor = codec.decode(codec.encode(expected), FeedSort.RELEVANCE);

        assertThat(cursor).isEqualTo(expected);
    }

    @Test
    void 잘못된_커서를_거부한다() {
        assertThatThrownBy(() -> codec.decode("invalid", FeedSort.LATEST))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 음수_좋아요_수가_포함된_커서를_거부한다() {
        FeedCursor cursor = new FeedCursor(
                FeedSort.POPULAR,
                0,
                -1L,
                Instant.parse("2026-09-11T00:00:00Z"),
                10L
        );

        assertThatThrownBy(() -> codec.decode(codec.encode(cursor), FeedSort.POPULAR))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 다른_정렬의_커서를_거부한다() {
        FeedCursor cursor = new FeedCursor(
                FeedSort.LATEST,
                0,
                0L,
                Instant.parse("2026-09-11T00:00:00Z"),
                10L
        );

        assertThatThrownBy(() -> codec.decode(codec.encode(cursor), FeedSort.POPULAR))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 검색이_아닌_커서에_정확도_등급이_있으면_거부한다() {
        FeedCursor cursor = new FeedCursor(
                FeedSort.LATEST,
                1,
                0L,
                Instant.parse("2026-09-11T00:00:00Z"),
                10L
        );

        assertThatThrownBy(() -> codec.decode(codec.encode(cursor), FeedSort.LATEST))
                .isInstanceOf(BadRequestException.class);
    }
}

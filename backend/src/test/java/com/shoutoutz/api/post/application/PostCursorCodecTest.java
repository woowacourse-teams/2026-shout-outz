package com.shoutoutz.api.post.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.post.application.dto.PostCursor;
import com.shoutoutz.api.post.application.dto.PostSort;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PostCursorCodecTest {

    private final PostCursorCodec codec = new PostCursorCodec();

    @Test
    void 좋아요_수와_생성_시각과_ID를_커서로_변환하고_복원한다() {
        Instant createdAt = Instant.parse("2026-09-11T00:00:00Z");
        PostCursor expected = new PostCursor(PostSort.POPULAR, 42L, createdAt, 10L);

        PostCursor cursor = codec.decode(codec.encode(expected), PostSort.POPULAR);

        assertThat(cursor).isEqualTo(expected);
    }

    @Test
    void 잘못된_커서를_거부한다() {
        assertThatThrownBy(() -> codec.decode("invalid", PostSort.LATEST))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 음수_좋아요_수가_포함된_커서를_거부한다() {
        PostCursor cursor = new PostCursor(
                PostSort.POPULAR,
                -1L,
                Instant.parse("2026-09-11T00:00:00Z"),
                10L
        );

        assertThatThrownBy(() -> codec.decode(codec.encode(cursor), PostSort.POPULAR))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 다른_정렬의_커서를_거부한다() {
        PostCursor cursor = new PostCursor(
                PostSort.LATEST,
                0L,
                Instant.parse("2026-09-11T00:00:00Z"),
                10L
        );

        assertThatThrownBy(() -> codec.decode(codec.encode(cursor), PostSort.POPULAR))
                .isInstanceOf(BadRequestException.class);
    }
}

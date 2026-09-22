package com.shoutoutz.api.feed.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FeedTest {

    private static final Instant NOW = Instant.parse("2026-09-11T00:00:00Z");

    @Test
    void 피드를_생성한다() {
        String title = "피드 제목";
        String content = "  첫 번째 줄\n두 번째 줄  \n";

        Feed feed = Feed.create(1L, title, content, NOW);

        assertThat(feed.getId()).isNull();
        assertThat(feed.getAuthorId()).isEqualTo(1L);
        assertThat(feed.getTitle()).isEqualTo(title);
        assertThat(feed.getContent()).isEqualTo(content);
        assertThat(feed.getCreatedAt()).isEqualTo(NOW);
        assertThat(feed.getUpdatedAt()).isEqualTo(NOW);
        assertThat(feed.getDeletedAt()).isNull();
    }

    @Test
    void 제목은_공백이거나_100자를_초과할_수_없다() {
        assertThatThrownBy(() -> Feed.create(1L, "   ", "본문", NOW))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Feed.create(1L, "가".repeat(101), "본문", NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void 공백으로만_이루어진_본문은_허용하지_않는다() {
        assertThatThrownBy(() -> Feed.create(1L, "제목", "   ", NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void 도메인_상태의_수정_시각은_생성_시각보다_앞설_수_없다() {
        assertThatThrownBy(() -> Feed.reconstitute(
                1L,
                1L,
                "제목",
                "본문",
                NOW,
                NOW.minusSeconds(1),
                null
        )).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void 피드_제목과_본문을_수정한다() {
        Feed feed = Feed.reconstitute(1L, 2L, "기존 제목", "기존 본문", NOW, NOW, null);

        Feed updated = feed.update("수정 제목", "수정 본문", NOW.plusSeconds(1));

        assertThat(updated.getTitle()).isEqualTo("수정 제목");
        assertThat(updated.getContent()).isEqualTo("수정 본문");
        assertThat(updated.getUpdatedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void 피드를_soft_delete한다() {
        Feed feed = Feed.reconstitute(1L, 2L, "제목", "본문", NOW, NOW, null);

        Feed deleted = feed.delete(NOW.plusSeconds(1));

        assertThat(deleted.getDeletedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(deleted.getUpdatedAt()).isEqualTo(NOW.plusSeconds(1));
    }
}

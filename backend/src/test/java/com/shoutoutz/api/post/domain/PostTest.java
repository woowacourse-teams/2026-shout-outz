package com.shoutoutz.api.post.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PostTest {

    private static final Instant NOW = Instant.parse("2026-09-11T00:00:00Z");

    @Test
    void 포스트를_생성한다() {
        String content = "  첫 번째 줄\n두 번째 줄  \n";

        Post post = Post.create(1L, content, NOW);

        assertThat(post.getId()).isNull();
        assertThat(post.getAuthorId()).isEqualTo(1L);
        assertThat(post.getContent()).isEqualTo(content);
        assertThat(post.getCreatedAt()).isEqualTo(NOW);
        assertThat(post.getUpdatedAt()).isEqualTo(NOW);
        assertThat(post.getDeletedAt()).isNull();
    }

    @Test
    void 공백으로만_이루어진_본문은_허용하지_않는다() {
        assertThatThrownBy(() -> Post.create(1L, "   ", NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void 도메인_상태의_수정_시각은_생성_시각보다_앞설_수_없다() {
        assertThatThrownBy(() -> Post.reconstitute(
                1L,
                1L,
                "본문",
                NOW,
                NOW.minusSeconds(1),
                null
        )).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void 포스트_본문을_수정한다() {
        Post post = Post.reconstitute(1L, 2L, "기존 본문", NOW, NOW, null);

        Post updated = post.updateContent("수정 본문", NOW.plusSeconds(1));

        assertThat(updated.getContent()).isEqualTo("수정 본문");
        assertThat(updated.getUpdatedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void 포스트를_soft_delete한다() {
        Post post = Post.reconstitute(1L, 2L, "본문", NOW, NOW, null);

        Post deleted = post.delete(NOW.plusSeconds(1));

        assertThat(deleted.getDeletedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(deleted.getUpdatedAt()).isEqualTo(NOW.plusSeconds(1));
    }
}

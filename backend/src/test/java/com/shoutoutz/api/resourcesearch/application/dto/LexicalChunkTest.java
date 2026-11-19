package com.shoutoutz.api.resourcesearch.application.dto;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LexicalChunkTest {

    private static final RankedResource RESOURCE = new RankedResource(10L, null, "github-pr");

    @Test
    void 트랙이_없는_조각을_만들_수_있다() {
        assertThatCode(() -> new LexicalChunk(1L, "본문", null, RESOURCE)).doesNotThrowAnyException();
    }

    @Test
    void 조각_ID가_없으면_거부한다() {
        assertThatThrownBy(() -> new LexicalChunk(null, "본문", ResourceTrack.BE, RESOURCE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    void 본문이_비어_있으면_거부한다(String text) {
        assertThatThrownBy(() -> new LexicalChunk(1L, text, ResourceTrack.BE, RESOURCE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 속한_자료가_없으면_거부한다() {
        assertThatThrownBy(() -> new LexicalChunk(1L, "본문", ResourceTrack.BE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.shoutoutz.api.common.embedding.EmbeddingClient;
import com.shoutoutz.api.common.embedding.EmbeddingException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SearchKeywordEmbeddingPreloaderTest {

    @Test
    @DisplayName("검색어 칩 8개를 한 번의 요청으로 임베딩한다")
    void embedsSuggestedKeywordsInOneBatch() {
        RecordingEmbeddingClient embeddingClient = new RecordingEmbeddingClient(false);

        new SearchKeywordEmbeddingPreloader(embeddingClient).preload();

        assertThat(embeddingClient.batches).containsExactly(ResourceQueryService.SUGGESTED_KEYWORDS);
        assertThat(ResourceQueryService.SUGGESTED_KEYWORDS).hasSize(8);
    }

    @Test
    @DisplayName("미리 임베딩하지 못해도 예외를 밖으로 던지지 않는다")
    void ignoresEmbeddingFailure() {
        RecordingEmbeddingClient embeddingClient = new RecordingEmbeddingClient(true);

        assertThatCode(() -> new SearchKeywordEmbeddingPreloader(embeddingClient).preload())
                .doesNotThrowAnyException();
    }

    private static final class RecordingEmbeddingClient implements EmbeddingClient {

        private final List<List<String>> batches = new ArrayList<>();
        private final boolean fail;

        private RecordingEmbeddingClient(boolean fail) {
            this.fail = fail;
        }

        @Override
        public float[] embed(String text) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<float[]> embedAll(List<String> texts) {
            batches.add(List.copyOf(texts));
            if (fail) {
                throw new EmbeddingException(503, "UNAVAILABLE", true);
            }
            return texts.stream().map(text -> new float[]{1f}).toList();
        }

        @Override
        public String modelId() {
            return "test-model@1";
        }
    }
}

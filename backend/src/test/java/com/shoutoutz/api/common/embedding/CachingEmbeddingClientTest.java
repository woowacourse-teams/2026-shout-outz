package com.shoutoutz.api.common.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class CachingEmbeddingClientTest {

    private final RecordingEmbeddingClient delegate = new RecordingEmbeddingClient();
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final CachingEmbeddingClient client = new CachingEmbeddingClient(delegate, 100, registry);

    @Test
    @DisplayName("같은 검색어는 한 번만 임베딩하고 이후에는 캐시에서 준다")
    void embedsSameTextOnce() {
        float[] first = client.embed("동시성");
        float[] second = client.embed("동시성");

        assertThat(delegate.embeddedTexts).containsExactly("동시성");
        assertThat(second).containsExactly(first);
    }

    @Test
    @DisplayName("다른 검색어는 각각 임베딩한다")
    void embedsDifferentTextsSeparately() {
        client.embed("동시성");
        client.embed("부하 테스트");

        assertThat(delegate.embeddedTexts).containsExactly("동시성", "부하 테스트");
    }

    @Test
    @DisplayName("돌려받은 벡터를 바꿔도 캐시에 든 벡터는 바뀌지 않는다")
    void keepsCachedVectorWhenCallerMutatesResult() {
        float[] first = client.embed("동시성");
        float original = first[0];
        first[0] = 99f;

        assertThat(client.embed("동시성")[0]).isEqualTo(original);
    }

    @Test
    @DisplayName("임베딩에 실패하면 캐시에 남기지 않고 다음 요청에서 다시 부른다")
    void doesNotCacheFailure() {
        delegate.failNext = true;

        assertThatThrownBy(() -> client.embed("동시성")).isInstanceOf(EmbeddingException.class);
        client.embed("동시성");

        assertThat(delegate.embeddedTexts).containsExactly("동시성", "동시성");
    }

    @Test
    @DisplayName("여러 문장을 한 번에 임베딩할 때는 캐시에 없는 것만 한 번의 요청으로 부른다")
    void embedsOnlyMissingTextsInOneBatch() {
        client.embed("동시성");

        List<float[]> vectors = client.embedAll(List.of("부하 테스트", "동시성", "LLM"));

        assertThat(delegate.batches).containsExactly(List.of("부하 테스트", "LLM"));
        assertThat(vectors).extracting(vector -> vector[0]).containsExactly(6f, 3f, 3f);
    }

    @Test
    @DisplayName("한 번에 임베딩한 문장은 이후 한 문장씩 요청해도 캐시에서 준다")
    void cachesTextsEmbeddedInBatch() {
        client.embedAll(List.of("동시성", "LLM"));

        client.embed("LLM");

        assertThat(delegate.embeddedTexts).isEmpty();
    }

    @Test
    @DisplayName("적중과 미적중 횟수를 search.embedding 캐시 지표로 남긴다")
    void recordsHitAndMissMetrics() {
        client.embed("동시성");
        client.embed("동시성");
        client.embed("LLM");

        assertThat(cacheGets("hit")).isEqualTo(1);
        assertThat(cacheGets("miss")).isEqualTo(2);
    }

    @Test
    @DisplayName("모델 ID는 감싼 클라이언트의 값을 그대로 준다")
    void delegatesModelId() {
        assertThat(client.modelId()).isEqualTo("test-model@4");
    }

    @Test
    @DisplayName("임베딩 설정은 캐시를 두른 클라이언트를 등록한다")
    void configurationRegistersCachingClient() {
        EmbeddingProperties properties = new EmbeddingProperties(
                "openai", "text-embedding-3-small", 1024, true, "key", Duration.ofSeconds(1), Duration.ofSeconds(1));

        EmbeddingClient registered = new EmbeddingConfiguration().embeddingClient(
                RestClient.builder(), properties, JsonMapper.builder().build(), new StaticListableBeanFactory().getBeanProvider(MeterRegistry.class));

        assertThat(registered).isInstanceOf(CachingEmbeddingClient.class);
    }

    private double cacheGets(String result) {
        return registry.get("cache.gets")
                .tag("cache", "search.embedding")
                .tag("result", result)
                .functionCounter()
                .count();
    }

    private static final class RecordingEmbeddingClient implements EmbeddingClient {

        private final List<String> embeddedTexts = new ArrayList<>();
        private final List<List<String>> batches = new ArrayList<>();
        private boolean failNext;

        @Override
        public float[] embed(String text) {
            embeddedTexts.add(text);
            if (failNext) {
                failNext = false;
                throw new EmbeddingException(503, "UNAVAILABLE", true);
            }
            return new float[]{text.length(), 1f, 0f, 0f};
        }

        @Override
        public List<float[]> embedAll(List<String> texts) {
            batches.add(List.copyOf(texts));
            return texts.stream()
                    .map(text -> new float[]{text.length(), 1f, 0f, 0f})
                    .toList();
        }

        @Override
        public String modelId() {
            return "test-model@4";
        }
    }
}

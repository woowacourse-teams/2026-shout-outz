package com.shoutoutz.api.common.embedding;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 같은 문장의 임베딩을 다시 부르지 않도록 앱 메모리에 담는다.
 * 감싼 클라이언트의 모델이 고정이라 문장만 키로 쓰고, 모델을 바꾸면 서버가 새로 떠서 캐시도 새로 만든다.
 * 벡터는 바꿀 수 있는 배열이라 넣을 때와 꺼낼 때 복사해 캐시 값을 지킨다.
 */
public final class CachingEmbeddingClient implements EmbeddingClient {

    private static final String CACHE_NAME = "search.embedding";

    private final EmbeddingClient delegate;
    private final Cache<String, float[]> cache;

    public CachingEmbeddingClient(EmbeddingClient delegate, long maximumSize, MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .recordStats()
                .build();
        CaffeineCacheMetrics.monitor(meterRegistry, cache, CACHE_NAME);
    }

    @Override
    public float[] embed(String text) {
        return cache.get(text, key -> delegate.embed(key).clone()).clone();
    }

    /**
     * 캐시에 없는 문장만 모아 한 번의 요청으로 임베딩해 담고, 입력 순서대로 돌려준다.
     * 담은 값은 반환 전에 밀려날 수 있어, 돌려줄 벡터는 캐시를 다시 읽지 않고 이 요청 안에서 모은다.
     */
    @Override
    public List<float[]> embedAll(List<String> texts) {
        Map<String, float[]> vectorsByText = new HashMap<>();
        for (String text : texts) {
            float[] cached = cache.getIfPresent(text);
            if (cached != null) {
                vectorsByText.put(text, cached);
            }
        }
        List<String> missing = texts.stream()
                .distinct()
                .filter(text -> !vectorsByText.containsKey(text))
                .toList();
        if (!missing.isEmpty()) {
            List<float[]> vectors = delegate.embedAll(missing);
            for (int i = 0; i < missing.size(); i++) {
                float[] vector = vectors.get(i).clone();
                cache.put(missing.get(i), vector);
                vectorsByText.put(missing.get(i), vector);
            }
        }
        return texts.stream()
                .map(text -> vectorsByText.get(text).clone())
                .toList();
    }

    @Override
    public String modelId() {
        return delegate.modelId();
    }
}

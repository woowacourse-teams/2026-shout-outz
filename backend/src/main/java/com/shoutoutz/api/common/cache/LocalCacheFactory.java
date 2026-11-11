package com.shoutoutz.api.common.cache;

import com.github.benmanes.caffeine.cache.Ticker;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * 설정에 맞춰 로컬 캐시를 만든다.
 * 캐시가 꺼져 있으면 값을 보관하지 않는 캐시를 돌려주므로, 쓰는 쪽은 켜짐 여부를 신경 쓰지 않아도 된다.
 */
public class LocalCacheFactory {

    private final LocalCacheProperties properties;
    private final Executor refreshExecutor;
    private final MeterRegistry meterRegistry;

    public LocalCacheFactory(LocalCacheProperties properties, Executor refreshExecutor, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.refreshExecutor = refreshExecutor;
        this.meterRegistry = meterRegistry;
    }

    /**
     * @param name   설정(local-cache.caches)의 캐시 이름. 적중률 지표의 cache 태그로도 쓴다.
     * @param loader 값이 없거나 갱신할 때 새 값을 읽어 오는 함수.
     */
    public <K, V> LocalCache<K, V> create(String name, Function<K, V> loader) {
        // 꺼져 있어도 설정을 먼저 찾는다. 캐시를 끄는 테스트에서도 이름이 틀리거나 설정이 빠진 것을 잡기 위함이다.
        LocalCacheProperties.Spec spec = properties.spec(name);
        if (!properties.enabled()) {
            return LocalCache.disabled(loader);
        }
        LocalCache<K, V> cache = LocalCache.caching(
                spec,
                loader,
                refreshExecutor,
                Ticker.systemTicker()
        );
        CaffeineCacheMetrics.monitor(meterRegistry, cache.caffeine(), name);
        return cache;
    }
}

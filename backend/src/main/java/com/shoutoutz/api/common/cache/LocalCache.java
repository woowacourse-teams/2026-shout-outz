package com.shoutoutz.api.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Ticker;
import java.util.concurrent.Executor;
import java.util.function.Function;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 조회 결과를 앱 서버 메모리에 보관하는 캐시.
 * 갱신 시간이 지난 값은 바로 버리지 않는다. 일단 돌려주고, 뒤에서 새 값을 읽어 와 바꾼다.
 * 새로 읽는 순간에 DB가 멈춰 있어도 요청이 기다리지 않게 하기 위함이다.
 * 값이 아예 없을 때만 요청이 직접 읽어 오고, 같은 키를 찾는 다른 요청은 그 결과를 기다린다.
 */
public final class LocalCache<K, V> {

    private final Function<K, V> loader;
    private final LoadingCache<K, V> cache;

    private LocalCache(Function<K, V> loader, LoadingCache<K, V> cache) {
        this.loader = loader;
        this.cache = cache;
    }

    static <K, V> LocalCache<K, V> caching(
            LocalCacheProperties.Spec spec,
            Function<K, V> loader,
            Executor executor,
            Ticker ticker
    ) {
        LoadingCache<K, V> cache = Caffeine.newBuilder()
                .refreshAfterWrite(spec.refreshAfter())
                .expireAfterWrite(spec.expireAfter())
                .maximumSize(spec.maximumSize())
                .executor(executor)
                .ticker(ticker)
                .recordStats()
                .build(loader::apply);
        return new LocalCache<>(loader, cache);
    }

    /**
     * 값을 보관하지 않고 매번 새로 읽는다.
     */
    static <K, V> LocalCache<K, V> disabled(Function<K, V> loader) {
        return new LocalCache<>(loader, null);
    }

    public V get(K key) {
        if (cache == null) {
            return loader.apply(key);
        }
        return cache.get(key);
    }

    /**
     * 보관한 값을 지운다. 다음 요청이 새로 읽어 온다.
     */
    public void evict(K key) {
        if (cache != null) {
            cache.invalidate(key);
        }
    }

    /**
     * 트랜잭션 안에서 부르면 커밋된 뒤에 지운다. 트랜잭션 밖이면 바로 지운다.
     * 커밋 전에 지우면, 그사이 들어온 조회가 아직 바뀌기 전 값을 읽어 다시 보관할 수 있다.
     * 롤백되면 값이 바뀌지 않았으므로 지우지 않는다.
     */
    public void evictAfterCommit(K key) {
        if (cache == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            evict(key);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evict(key);
            }
        });
    }

    LoadingCache<K, V> caffeine() {
        return cache;
    }
}

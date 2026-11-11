package com.shoutoutz.api.common.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocalCacheTest {

    private static final LocalCacheProperties.Spec SPEC =
            new LocalCacheProperties.Spec(Duration.ofSeconds(60), Duration.ofSeconds(300), 10);

    private final AtomicLong nanos = new AtomicLong();
    private final Ticker ticker = nanos::get;
    /** 뒤에서 하는 갱신을 테스트가 원하는 때에 실행하려고 모아 둔다. */
    private final Queue<Runnable> refreshTasks = new ArrayDeque<>();
    private final AtomicInteger loadCount = new AtomicInteger();
    private final Function<String, String> loader = key -> key + "-" + loadCount.incrementAndGet();

    @Test
    @DisplayName("같은 키를 다시 조회하면 새로 읽지 않고 보관한 값을 돌려준다")
    void get_returnsCachedValue() {
        LocalCache<String, String> cache = cache();

        String first = cache.get("banner");
        String second = cache.get("banner");

        assertThat(first).isEqualTo("banner-1");
        assertThat(second).isEqualTo("banner-1");
        assertThat(loadCount).hasValue(1);
    }

    @Test
    @DisplayName("키가 다르면 따로 보관한다")
    void get_keepsValuesPerKey() {
        LocalCache<String, String> cache = cache();

        cache.get("user-1");
        cache.get("user-2");

        assertThat(cache.get("user-1")).isEqualTo("user-1-1");
        assertThat(cache.get("user-2")).isEqualTo("user-2-2");
        assertThat(loadCount).hasValue(2);
    }

    @Test
    @DisplayName("갱신 시간이 지난 값은 일단 돌려주고, 뒤에서 새로 읽어 바꾼다")
    void get_returnsStaleValueAndRefreshesBehind() {
        LocalCache<String, String> cache = cache();
        cache.get("banner");

        advance(Duration.ofSeconds(61));
        String stale = cache.get("banner");

        assertThat(stale).isEqualTo("banner-1");
        assertThat(loadCount).hasValue(1);

        runRefreshTasks();

        assertThat(loadCount).hasValue(2);
        assertThat(cache.get("banner")).isEqualTo("banner-2");
    }

    @Test
    @DisplayName("뒤에서 새로 읽다가 실패하면 예전 값을 그대로 돌려준다")
    void get_keepsStaleValueWhenRefreshFails() {
        AtomicInteger calls = new AtomicInteger();
        LocalCache<String, String> cache = cache(key -> {
            if (calls.incrementAndGet() > 1) {
                throw new IllegalStateException("DB 조회 실패");
            }
            return "first";
        });
        cache.get("banner");

        advance(Duration.ofSeconds(61));
        cache.get("banner");
        runRefreshTasks();

        assertThat(calls).hasValue(2);
        assertThat(cache.get("banner")).isEqualTo("first");
    }

    @Test
    @DisplayName("상한이 지난 값은 돌려주지 않고 새로 읽는다")
    void get_reloadsAfterExpire() {
        LocalCache<String, String> cache = cache();
        cache.get("banner");

        advance(Duration.ofSeconds(301));

        assertThat(cache.get("banner")).isEqualTo("banner-2");
        assertThat(loadCount).hasValue(2);
    }

    @Test
    @DisplayName("값을 지우면 다음 조회가 새로 읽는다")
    void evict_removesValue() {
        LocalCache<String, String> cache = cache();
        cache.get("user-1");
        cache.get("user-2");

        cache.evict("user-1");

        assertThat(cache.get("user-1")).isEqualTo("user-1-3");
        assertThat(cache.get("user-2")).isEqualTo("user-2-2");
    }

    @Test
    @DisplayName("값이 없을 때 읽다가 실패하면 예외를 그대로 던지고, 실패는 보관하지 않는다")
    void get_propagatesLoadFailure() {
        AtomicInteger calls = new AtomicInteger();
        LocalCache<String, String> cache = cache(key -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("DB 조회 실패");
            }
            return "second";
        });

        assertThatThrownBy(() -> cache.get("banner")).isInstanceOf(IllegalStateException.class);
        assertThat(cache.get("banner")).isEqualTo("second");
    }

    @Test
    @DisplayName("값이 없을 때 같은 키를 동시에 조회하면 한 요청만 읽고, 나머지는 그 결과를 기다린다")
    void get_loadsOnceForConcurrentRequests() throws Exception {
        CountDownLatch loading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        LocalCache<String, String> cache = cache(key -> {
            calls.incrementAndGet();
            loading.countDown();
            await(release);
            return "loaded";
        });

        try (ExecutorService requests = Executors.newFixedThreadPool(2)) {
            Future<String> first = requests.submit(() -> cache.get("banner"));
            assertThat(loading.await(5, TimeUnit.SECONDS)).isTrue();
            Future<String> second = requests.submit(() -> cache.get("banner"));
            release.countDown();

            assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo("loaded");
            assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo("loaded");
        }
        assertThat(calls).hasValue(1);
    }

    @Test
    @DisplayName("캐시를 끄면 보관하지 않고 매번 새로 읽는다")
    void disabled_loadsEveryTime() {
        LocalCache<String, String> cache = LocalCache.disabled(loader);

        cache.get("banner");
        cache.get("banner");
        cache.evict("banner");

        assertThat(cache.get("banner")).isEqualTo("banner-3");
        assertThat(loadCount).hasValue(3);
    }

    private LocalCache<String, String> cache() {
        return cache(loader);
    }

    private LocalCache<String, String> cache(Function<String, String> loader) {
        return LocalCache.caching(SPEC, loader, refreshTasks::add, ticker);
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private void advance(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }

    private void runRefreshTasks() {
        Runnable task;
        while ((task = refreshTasks.poll()) != null) {
            task.run();
        }
    }
}

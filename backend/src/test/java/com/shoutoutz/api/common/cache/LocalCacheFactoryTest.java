package com.shoutoutz.api.common.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocalCacheFactoryTest {

    private static final Map<String, LocalCacheProperties.Spec> CACHES = Map.of(
            "home-banners",
            new LocalCacheProperties.Spec(Duration.ofSeconds(60), Duration.ofSeconds(300), 1)
    );

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final AtomicInteger loadCount = new AtomicInteger();

    @Test
    @DisplayName("켜져 있으면 값을 보관하고, 적중과 실패 횟수를 캐시 이름 태그로 지표에 남긴다")
    void create_cachesAndRecordsMetricsWhenEnabled() {
        LocalCache<String, Integer> cache = factory(true).create("home-banners", key -> loadCount.incrementAndGet());

        cache.get("all");
        cache.get("all");

        assertThat(loadCount).hasValue(1);
        assertThat(gets("miss")).isEqualTo(1);
        assertThat(gets("hit")).isEqualTo(1);
    }

    @Test
    @DisplayName("꺼져 있으면 값을 보관하지 않고 지표도 남기지 않는다")
    void create_doesNotCacheWhenDisabled() {
        LocalCache<String, Integer> cache = factory(false).create("home-banners", key -> loadCount.incrementAndGet());

        cache.get("all");
        cache.get("all");

        assertThat(loadCount).hasValue(2);
        assertThat(meterRegistry.find("cache.gets").meters()).isEmpty();
    }

    @Test
    @DisplayName("설정에 없는 캐시 이름이면 꺼져 있어도 예외가 발생한다")
    void create_throwsWhenSpecIsMissingEvenIfDisabled() {
        assertThatThrownBy(() -> factory(false).create("unknown", key -> key))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> factory(true).create("unknown", key -> key))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private LocalCacheFactory factory(boolean enabled) {
        return new LocalCacheFactory(new LocalCacheProperties(enabled, CACHES), Runnable::run, meterRegistry);
    }

    private double gets(String result) {
        return meterRegistry.get("cache.gets")
                .tag("cache", "home-banners")
                .tag("result", result)
                .functionCounter()
                .count();
    }
}

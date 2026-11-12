package com.shoutoutz.api.common.cache;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.transaction.support.TransactionOperations;

/**
 * 서비스 테스트에서 쓰는 로컬 캐시 팩토리.
 * 갱신은 요청 스레드에서 바로 실행하고, 트랜잭션은 열지 않는다.
 */
public final class TestLocalCacheFactory {

    private static final LocalCacheProperties.Spec SPEC =
            new LocalCacheProperties.Spec(Duration.ofMinutes(1), Duration.ofMinutes(5), 100);

    private TestLocalCacheFactory() {
    }

    public static LocalCacheFactory enabled(String... names) {
        return create(true, names);
    }

    public static LocalCacheFactory disabled(String... names) {
        return create(false, names);
    }

    private static LocalCacheFactory create(boolean enabled, String... names) {
        LocalCacheProperties properties = new LocalCacheProperties(
                enabled,
                Arrays.stream(names).collect(Collectors.toMap(Function.identity(), name -> SPEC))
        );
        return new LocalCacheFactory(
                properties,
                Runnable::run,
                new SimpleMeterRegistry(),
                TransactionOperations.withoutTransaction()
        );
    }
}

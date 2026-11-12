package com.shoutoutz.api.common.config;

import com.shoutoutz.api.common.cache.LocalCacheFactory;
import com.shoutoutz.api.common.cache.LocalCacheProperties;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LocalCacheProperties.class)
public class CacheConfig {

    private static final int REFRESH_THREADS = 2;

    /**
     * 갱신 시간이 지난 값을 뒤에서 새로 읽는 전용 스레드.
     * 요청 스레드와 분리해 두어, DB가 느려져도 갱신이 요청을 붙잡지 않는다.
     * 스레드 수만큼만 커넥션을 쓰므로 갱신이 커넥션 풀을 차지하는 양도 제한된다.
     */
    @Bean(destroyMethod = "shutdown")
    ExecutorService localCacheRefreshExecutor() {
        AtomicInteger sequence = new AtomicInteger();
        return Executors.newFixedThreadPool(REFRESH_THREADS, task -> {
            Thread thread = new Thread(task, "local-cache-refresh-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    @Bean
    LocalCacheFactory localCacheFactory(
            LocalCacheProperties properties,
            ExecutorService localCacheRefreshExecutor,
            MeterRegistry meterRegistry,
            PlatformTransactionManager transactionManager
    ) {
        TransactionTemplate readOnlyTransaction = new TransactionTemplate(transactionManager);
        readOnlyTransaction.setReadOnly(true);
        return new LocalCacheFactory(properties, localCacheRefreshExecutor, meterRegistry, readOnlyTransaction);
    }
}

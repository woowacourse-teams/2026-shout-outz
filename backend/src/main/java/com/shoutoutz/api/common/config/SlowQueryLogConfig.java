package com.shoutoutz.api.common.config;

import com.shoutoutz.api.common.logging.SlowQueryLoggingListener;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * slow-query-log.enabled=true일 때만 DataSource를 datasource-proxy로 감싸 느린 쿼리를 로그로 남긴다.
 * 부하 테스트에서 커넥션을 오래 잡는 쿼리를 찾기 위한 설정이라 기본은 꺼져 있다. (loadtest 프로필에서만 켠다)
 * 히카리 지표는 감싼 DataSource를 풀어서(unwrap) 원래 HikariDataSource를 찾으므로 그대로 수집된다.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "slow-query-log.enabled", havingValue = "true")
public class SlowQueryLogConfig {

    private static final String THRESHOLD_PROPERTY = "slow-query-log.threshold-ms";
    private static final long DEFAULT_THRESHOLD_MILLIS = 50;

    /**
     * BeanPostProcessor는 다른 빈보다 먼저 만들어지므로 static으로 두고, 설정값은 Environment에서 직접 읽는다.
     */
    @Bean
    static BeanPostProcessor slowQueryLoggingDataSourcePostProcessor(Environment environment) {
        long thresholdMillis = environment.getProperty(THRESHOLD_PROPERTY, Long.class, DEFAULT_THRESHOLD_MILLIS);
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (!(bean instanceof DataSource dataSource) || bean instanceof ProxyDataSource) {
                    return bean;
                }
                return ProxyDataSourceBuilder.create(dataSource)
                        .name(beanName)
                        .listener(new SlowQueryLoggingListener(thresholdMillis))
                        .build();
            }
        };
    }
}

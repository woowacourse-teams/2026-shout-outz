package com.shoutoutz.api.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import javax.sql.DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SlowQueryLogConfigTest {

    private final DataSource originalDataSource = mock(DataSource.class);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean("dataSource", DataSource.class, () -> originalDataSource)
            .withUserConfiguration(SlowQueryLogConfig.class);

    @Test
    @DisplayName("설정이 없으면 DataSource를 감싸지 않는다.")
    void doesNotWrapDataSourceByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(DataSource.class)).isSameAs(originalDataSource);
        });
    }

    @ParameterizedTest(name = "slow-query-log.enabled={0}")
    @ValueSource(strings = {"false", ""})
    @DisplayName("켜져 있지 않으면 DataSource를 감싸지 않는다.")
    void doesNotWrapDataSourceWhenDisabled(String enabled) {
        contextRunner.withPropertyValues("slow-query-log.enabled=" + enabled).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(DataSource.class)).isSameAs(originalDataSource);
        });
    }

    @Test
    @DisplayName("켜져 있으면 DataSource를 datasource-proxy로 감싼다.")
    void wrapsDataSourceWhenEnabled() {
        contextRunner.withPropertyValues("slow-query-log.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            DataSource dataSource = context.getBean(DataSource.class);
            assertThat(dataSource).isInstanceOf(ProxyDataSource.class);
            assertThat(((ProxyDataSource) dataSource).getDataSource()).isSameAs(originalDataSource);
        });
    }
}

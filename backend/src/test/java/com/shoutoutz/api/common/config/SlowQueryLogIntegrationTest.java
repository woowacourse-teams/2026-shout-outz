package com.shoutoutz.api.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.shoutoutz.api.common.logging.SlowQueryLoggingListener;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "aws.s3.bucket=test-bucket",
        "aws.s3.region=ap-northeast-2",
        "aws.s3.presigned-url-expiration-seconds=300",
        "spring.flyway.ignore-migration-patterns=*:missing",
        "slow-query-log.enabled=true",
        "slow-query-log.threshold-ms=50"
})
class SlowQueryLogIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    private Logger logger;
    private ListAppender<ILoggingEvent> logAppender;
    private Level previousLevel;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(SlowQueryLoggingListener.class);
        previousLevel = logger.getLevel();
        logger.setLevel(Level.TRACE);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logger.setLevel(previousLevel);
    }

    @Test
    @DisplayName("켜져 있으면 히카리 DataSource를 감싸고, 기준 시간을 넘은 쿼리를 로그로 남긴다.")
    void logsSlowQueryThroughWrappedHikariDataSource() {
        assertThat(dataSource).isInstanceOf(ProxyDataSource.class);
        assertThat(((ProxyDataSource) dataSource).getDataSource()).isInstanceOf(HikariDataSource.class);

        jdbcTemplate.queryForList("select pg_sleep(0.1)");

        assertThat(logAppender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> assertThat(message)
                        .startsWith("slow_query duration_ms=")
                        .endsWith("sql=select pg_sleep(0.1)"));
    }

    @Test
    @DisplayName("DataSource를 감싸도 히카리 커넥션 지표가 그대로 수집된다.")
    void keepsHikariMetricsAfterWrapping() {
        jdbcTemplate.queryForObject("select 1", Integer.class);

        assertThat(meterRegistry.find("hikaricp.connections.active").gauge()).isNotNull();
        assertThat(meterRegistry.find("hikaricp.connections.pending").gauge()).isNotNull();
        assertThat(meterRegistry.find("hikaricp.connections.usage").timer()).isNotNull();
        assertThat(meterRegistry.find("hikaricp.connections.acquire").timer()).isNotNull();
    }
}

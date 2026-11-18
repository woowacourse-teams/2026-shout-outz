package com.shoutoutz.api.resourcesearch.infrastructure;

import static com.shoutoutz.api.resourcesearch.support.ResourceSearchFixture.MODEL;
import static com.shoutoutz.api.resourcesearch.support.ResourceSearchFixture.vector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.ServiceUnavailableException;
import com.shoutoutz.api.resourcesearch.application.ResourceSearchRepository;
import com.shoutoutz.api.resourcesearch.domain.ResourceSearchErrorCode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@SpringBootTest
@TestPropertySource(properties = "resource.search.query-timeout=300ms")
@Timeout(20)
class JdbcResourceSearchRepositoryTimeoutIntegrationTest {

    @Autowired
    private ResourceSearchRepository resourceSearchRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 벡터_조회가_제한_시간을_넘으면_기다리지_않고_검색_지연_오류로_끝낸다() throws SQLException {
        try (Connection blocker = dataSource.getConnection()) {
            blocker.setAutoCommit(false);
            try (Statement statement = blocker.createStatement()) {
                statement.execute("LOCK TABLE resource_chunks IN ACCESS EXCLUSIVE MODE");
            }
            long startedAt = System.nanoTime();

            assertThatThrownBy(() -> resourceSearchRepository.findNearest(vector(1, 0), MODEL, null, 10))
                    .isInstanceOf(ServiceUnavailableException.class)
                    .extracting(exception -> ((ServiceUnavailableException) exception).getErrorCode())
                    .isEqualTo(ResourceSearchErrorCode.RESOURCE_SEARCH_TIMEOUT);

            assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isLessThan(Duration.ofSeconds(3));
            blocker.rollback();
        }
    }

    @Test
    void 제한_시간에_걸린_뒤에도_같은_커넥션_풀로_다음_조회를_할_수_있다() throws SQLException {
        try (Connection blocker = dataSource.getConnection()) {
            blocker.setAutoCommit(false);
            try (Statement statement = blocker.createStatement()) {
                statement.execute("LOCK TABLE resource_chunks IN ACCESS EXCLUSIVE MODE");
            }
            assertThatThrownBy(() -> resourceSearchRepository.findNearest(vector(1, 0), MODEL, null, 10))
                    .isInstanceOf(ServiceUnavailableException.class);
            blocker.rollback();
        }

        assertThat(resourceSearchRepository.findNearest(vector(1, 0), MODEL, null, 10)).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT current_setting('statement_timeout')", String.class))
                .isEqualTo("0");
    }
}

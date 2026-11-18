package com.shoutoutz.api.resourcesearch.infrastructure;

import static com.shoutoutz.api.resourcesearch.support.ResourceSearchFixture.MODEL;
import static com.shoutoutz.api.resourcesearch.support.ResourceSearchFixture.vector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.ServiceUnavailableException;
import com.shoutoutz.api.resourcesearch.application.ResourceSearchRepository;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.domain.ResourceSearchErrorCode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
@TestPropertySource(properties = {
        "resource.search.query-timeout=1s",
        "resource.search.max-concurrent=1",
        "resource.search.acquire-timeout=100ms"
})
@Timeout(20)
class JdbcResourceSearchRepositoryGuardIntegrationTest {

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

    @Test
    void 동시_실행_상한만큼_조회가_돌고_있으면_잠깐_기다린_뒤_혼잡_오류로_끝낸다() throws Exception {
        try (Connection blocker = dataSource.getConnection()) {
            blocker.setAutoCommit(false);
            try (Statement statement = blocker.createStatement()) {
                statement.execute("LOCK TABLE resource_chunks IN ACCESS EXCLUSIVE MODE");
            }
            CompletableFuture<Throwable> running = CompletableFuture.supplyAsync(this::searchFailure);
            waitUntilSearchIsBlocked(blocker);
            long startedAt = System.nanoTime();

            assertThatThrownBy(() -> resourceSearchRepository.findNearest(vector(1, 0), MODEL, null, 10))
                    .isInstanceOf(ServiceUnavailableException.class)
                    .extracting(exception -> ((ServiceUnavailableException) exception).getErrorCode())
                    .isEqualTo(ResourceSearchErrorCode.RESOURCE_SEARCH_BUSY);

            assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isLessThan(Duration.ofMillis(700));
            assertThat(running.get(5, TimeUnit.SECONDS)).isInstanceOf(ServiceUnavailableException.class);
            blocker.rollback();
        }
    }

    @Test
    void 조회가_실패로_끝나도_자리를_돌려줘_다음_조회를_받는다() throws SQLException {
        try (Connection blocker = dataSource.getConnection()) {
            blocker.setAutoCommit(false);
            try (Statement statement = blocker.createStatement()) {
                statement.execute("LOCK TABLE resource_chunks IN ACCESS EXCLUSIVE MODE");
            }
            assertThatThrownBy(() -> resourceSearchRepository.findNearest(vector(1, 0), MODEL, null, 10))
                    .isInstanceOf(ServiceUnavailableException.class);
            blocker.rollback();
        }

        List<ResourceSearchCandidate> first = resourceSearchRepository.findNearest(vector(1, 0), MODEL, null, 10);
        List<ResourceSearchCandidate> second = resourceSearchRepository.findNearest(vector(1, 0), MODEL, null, 10);

        assertThat(first).isEmpty();
        assertThat(second).isEmpty();
    }

    private Throwable searchFailure() {
        try {
            resourceSearchRepository.findNearest(vector(1, 0), MODEL, null, 10);
            return null;
        } catch (RuntimeException exception) {
            return exception;
        }
    }

    // 먼저 시작한 조회가 잠금 앞에서 멈춰 자리를 차지한 시점까지 기다린다.
    private void waitUntilSearchIsBlocked(Connection blocker) throws SQLException, InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        try (Statement statement = blocker.createStatement()) {
            while (System.nanoTime() < deadline) {
                // 잠금을 쥔 트랜잭션 안에서는 세션 상태 조회가 첫 결과로 고정되므로 조회마다 비운다.
                statement.execute("SELECT pg_stat_clear_snapshot()");
                try (var resultSet = statement.executeQuery(
                        "SELECT count(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock'")) {
                    resultSet.next();
                    if (resultSet.getInt(1) > 0) {
                        return;
                    }
                }
                Thread.sleep(10);
            }
        }
        throw new IllegalStateException("조회가 잠금에서 대기하지 않았습니다.");
    }
}

package com.shoutoutz.api.magazine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.magazine.application.MagazineService;
import com.shoutoutz.api.magazine.domain.MagazineRepository;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class MagazineViewIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MagazineService magazineService;

    @Autowired
    private MagazineRepository magazineRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private Clock clock;

    private long userId;
    private long magazineId;

    @BeforeEach
    void setUp() {
        given(clock.instant()).willReturn(Instant.parse("2026-10-15T12:00:00Z"));
        userId = jdbcTemplate.queryForObject("INSERT INTO users(handle, role) VALUES (?, 'ADMIN') RETURNING id", Long.class,
                "@i" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        magazineId = jdbcTemplate.queryForObject("""
                INSERT INTO magazines(title, featured_user_id, author_id, status, published_at)
                VALUES ('조회 검증', ?, ?, 'PUBLISHED', now()) RETURNING id
                """, Long.class, userId, userId);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM magazines WHERE id = ?", magazineId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    void 같은_방문자의_조회는_한국_날짜가_바뀌면_다시_집계한다() {
        VisitorKey visitor = new VisitorKey("a".repeat(64));
        assertThat(recordAt(visitor, "2026-10-15T14:59:59Z")).isEqualTo(1);
        assertThat(recordAt(visitor, "2026-10-15T14:59:59Z")).isEqualTo(1);
        assertThat(recordAt(visitor, "2026-10-15T15:00:00Z")).isEqualTo(2);
        assertThat(jdbcTemplate.queryForList("SELECT viewed_on::text FROM magazine_view_days WHERE magazine_id = ? ORDER BY viewed_on",
                String.class, magazineId)).containsExactly("2026-10-15", "2026-10-16");
        assertThat(jdbcTemplate.queryForList("SELECT visitor_key_hash FROM magazine_view_days WHERE magazine_id = ?",
                String.class, magazineId)).containsOnly(visitor.hash());
    }

    @Test
    void 같은_방문자가_동시에_요청해도_조회수는_한번만_오른다() throws Exception {
        var updatedAt = jdbcTemplate.queryForObject("SELECT updated_at FROM magazines WHERE id = ?", Instant.class, magazineId);
        VisitorKey visitor = new VisitorKey("b".repeat(64));
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> magazineService.find(magazineId, null, visitor).viewCount());
        }
        assertThat(run(tasks)).containsOnly(1L);
        assertThat(count()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT updated_at FROM magazines WHERE id = ?", Instant.class, magazineId))
                .isEqualTo(updatedAt);
    }

    @Test
    void 서로_다른_방문자의_동시_조회는_모두_집계한다() throws Exception {
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            VisitorKey visitor = new VisitorKey("visitor-" + i);
            tasks.add(() -> magazineService.find(magazineId, null, visitor).viewCount());
        }
        run(tasks);
        assertThat(count()).isEqualTo(8);
    }

    @Test
    void 이미_집계한_방문자의_조회는_매거진_행_락을_기다리지_않는다() throws Exception {
        VisitorKey visitor = new VisitorKey("already-viewed");
        assertThat(magazineService.find(magazineId, null, visitor).viewCount()).isEqualTo(1);
        var executor = Executors.newSingleThreadExecutor();
        try (var connection = jdbcTemplate.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            var lockedJdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            lockedJdbc.queryForObject("SELECT id FROM magazines WHERE id = ? FOR UPDATE", Long.class, magazineId);
            try {
                var response = executor.submit(() -> magazineService.find(magazineId, null, visitor));
                assertThat(response.get(3, TimeUnit.SECONDS).viewCount()).isEqualTo(1);
            } finally {
                connection.rollback();
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void 중복_기록이_선행_트랜잭션을_기다렸어도_커밋된_조회수를_반환한다() throws Exception {
        VisitorKey visitor = new VisitorKey("concurrent-duplicate");
        var executor = Executors.newSingleThreadExecutor();
        try (var connection = jdbcTemplate.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            var firstJdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            firstJdbc.update("""
                    INSERT INTO magazine_view_days(magazine_id, visitor_key_hash, viewed_on, first_seen_at)
                    VALUES (?, ?, '2026-10-15', now())
                    """, magazineId, visitor.hash());
            firstJdbc.update("UPDATE magazines SET view_count = view_count + 1 WHERE id = ?", magazineId);
            try {
                var backendPid = new CompletableFuture<Integer>();
                var response = executor.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                    backendPid.complete(jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Integer.class));
                    return magazineRepository.recordView(magazineId, visitor,
                            LocalDate.parse("2026-10-15"), Instant.parse("2026-10-15T12:00:00Z"));
                }));
                int pid = backendPid.get(5, TimeUnit.SECONDS);
                await().atMost(Duration.ofSeconds(5)).until(() -> jdbcTemplate.queryForObject(
                        "SELECT cardinality(pg_blocking_pids(?)) > 0", Boolean.class, pid));
                connection.commit();
                assertThat(response.get(5, TimeUnit.SECONDS)).isEqualTo(1);
                assertThat(count()).isEqualTo(1);
            } finally {
                connection.rollback();
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void 상세_조회_도중_발행이_취소되면_조회_기록도_롤백한다() {
        var adminTransaction = new TransactionTemplate(transactionManager);
        adminTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        given(clock.instant()).willAnswer(invocation -> {
            adminTransaction.executeWithoutResult(status ->
                    jdbcTemplate.update("UPDATE magazines SET status = 'DRAFT' WHERE id = ?", magazineId));
            return Instant.parse("2026-10-15T12:00:00Z");
        });

        assertThatThrownBy(() -> magazineService.find(magazineId, null, new VisitorKey("unpublished")))
                .isInstanceOf(EntityNotFoundException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM magazines WHERE id = ?", String.class, magazineId))
                .isEqualTo("DRAFT");
        assertThat(count()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM magazine_view_days WHERE magazine_id = ?",
                Long.class, magazineId)).isZero();
    }

    @Test
    void 관리자_원본과_목록_조회는_집계하지_않는다() {
        magazineService.findAdmin(magazineId, userId);
        magazineService.findAll(null, 20);
        magazineService.findAllAdmin(userId, null, 20);
        assertThat(count()).isZero();
    }

    private long recordAt(VisitorKey visitor, String timestamp) {
        given(clock.instant()).willReturn(Instant.parse(timestamp));
        return magazineService.find(magazineId, null, visitor).viewCount();
    }

    private long count() {
        return jdbcTemplate.queryForObject("SELECT view_count FROM magazines WHERE id = ?", Long.class, magazineId);
    }

    private List<Long> run(List<Callable<Long>> tasks) throws Exception {
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<Long> results = new ArrayList<>();
            for (var future : executor.invokeAll(tasks, 15, TimeUnit.SECONDS)) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}

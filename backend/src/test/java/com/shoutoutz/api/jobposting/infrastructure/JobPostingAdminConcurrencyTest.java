package com.shoutoutz.api.jobposting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.jobposting.domain.JobPosting;
import com.shoutoutz.api.jobposting.domain.JobPostingRepository;
import com.shoutoutz.api.jobposting.domain.JobRequirement;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 분석 Worker 가 결과를 저장하는 중에 공고를 지우거나 고쳐도 서로 기다리다 멈추지 않는지 확인한다.
 * 실제로 커밋돼야 잠금이 부딪치므로 테스트 트랜잭션을 쓰지 않고 직접 지운다.
 */
@ActiveProfiles("test")
@SpringBootTest
class JobPostingAdminConcurrencyTest {

    private static final long TIMEOUT_SECONDS = 5;
    private static final long POLL_MILLIS = 20;
    private static final Instant NOW = Instant.parse("2026-10-29T01:00:00Z");

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private long adminId;
    private long postingId;
    private long requirementId;
    private String sourceUrl;

    @BeforeEach
    void setUp() {
        adminId = jdbcTemplate.queryForObject("INSERT INTO users (handle) VALUES (?) RETURNING id", Long.class,
                "@d" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        sourceUrl = "https://example.com/jobs/" + UUID.randomUUID();
        postingId = jdbcTemplate.queryForObject("""
                INSERT INTO job_postings (source_url, content_hash, company_name, job_role, title, status,
                                          registered_by, created_at, updated_at)
                VALUES (?, repeat('a', 64), '우테코', 'BACKEND', '백엔드 개발자', 'PROCESSING', ?, now(), now())
                RETURNING id
                """, Long.class, sourceUrl, adminId);
        requirementId = jdbcTemplate.queryForObject("""
                INSERT INTO job_requirements (job_posting_id, display_order, name, category, summary, status,
                                              attempt_count, created_at, updated_at)
                VALUES (?, 1, 'Spring', 'REQUIRED', '요약', 'PROCESSING', 1, now(), now())
                RETURNING id
                """, Long.class, postingId);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM job_postings WHERE id = ?", postingId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", adminId);
    }

    @Test
    void Worker_가_결과를_저장하는_중에_지우면_저장이_끝날_때까지_기다렸다가_지운다() throws Exception {
        boolean deleted = runWhileWorkerSaving(() -> jobPostingRepository.deleteById(postingId));

        assertThat(deleted).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM job_requirements WHERE job_posting_id = ?", Integer.class, postingId)).isZero();
    }

    @Test
    void Worker_가_결과를_저장하는_중에_원문을_바꾸면_저장이_끝날_때까지_기다렸다가_역량을_새로_만든다() throws Exception {
        runWhileWorkerSaving(() -> new TransactionTemplate(transactionManager).execute(status -> {
            JobPosting saved = jobPostingRepository.findByIdForUpdate(postingId).orElseThrow();
            JobPosting updated = saved.update(sourceUrl, "바뀐 공고 원문", "우테코", JobRole.BACKEND, null,
                    "백엔드 개발자", null, null,
                    List.of(JobRequirement.create(1, "Kotlin", RequirementCategory.REQUIRED, "Kotlin 개발 경험")), NOW);
            jobPostingRepository.update(updated, true);
            return null;
        }));

        assertThat(jdbcTemplate.queryForList(
                "SELECT name, status FROM job_requirements WHERE job_posting_id = ?", postingId))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.get("name")).isEqualTo("Kotlin");
                    assertThat(row.get("status")).isEqualTo("PENDING");
                });
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM job_postings WHERE id = ?", String.class, postingId))
                .isEqualTo("PENDING");
    }

    /**
     * Worker 의 완료 저장처럼 역량 행을 먼저 잠그고, 관리자 작업이 잠금을 기다리기 시작한 뒤 공고 행을 잠근다.
     * 관리자 작업이 공고 행부터 잠그면 서로 기다리다 한쪽이 데드락으로 실패한다.
     */
    private <T> T runWhileWorkerSaving(Callable<T> adminTask) throws Exception {
        CountDownLatch requirementLocked = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> worker = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(
                    status -> {
                        jdbcTemplate.update("UPDATE job_requirements SET status = 'READY' WHERE id = ?",
                                requirementId);
                        requirementLocked.countDown();
                        awaitSessionWaitingForLock();
                        jdbcTemplate.queryForObject("SELECT id FROM job_postings WHERE id = ? FOR UPDATE",
                                Long.class, postingId);
                    }));
            Future<T> admin = executor.submit(() -> {
                if (!requirementLocked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Worker 가 역량 행을 잠그지 못했습니다.");
                }
                return adminTask.call();
            });

            worker.get(10, TimeUnit.SECONDS);
            return admin.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 시간을 정해 두고 기다리면 느린 환경에서 관리자 작업이 잠금을 기다리기 전에 공고 행을 잠가, 겹치는 경우를 확인하지 못하고 통과한다.
     * 그래서 다른 세션이 잠금을 기다리는 게 DB 에 보일 때까지 기다린다.
     */
    private void awaitSessionWaitingForLock() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (System.nanoTime() < deadline) {
            Integer waiting = jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE datname = current_database() AND wait_event_type = 'Lock'
                    """, Integer.class);
            if (waiting != null && waiting > 0) {
                return;
            }
            try {
                Thread.sleep(POLL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("관리자 작업이 역량 행 잠금을 기다리지 않았습니다.");
    }
}

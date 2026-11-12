package com.shoutoutz.api.jobposting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.jobposting.application.JobPostingAdminService;
import com.shoutoutz.api.jobposting.domain.JobPosting;
import com.shoutoutz.api.jobposting.domain.JobPostingErrorCode;
import com.shoutoutz.api.jobposting.domain.JobPostingRepository;
import com.shoutoutz.api.jobposting.domain.JobRequirement;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.ReanalysisScope;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingReanalysisRequest;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingReanalysisResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
 * 분석 Worker 가 결과를 저장하는 중에 공고를 지우거나 고치거나 다시 분석해도 서로 기다리다 멈추지 않는지 확인한다.
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
    private JobPostingAdminService jobPostingAdminService;

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
                    "백엔드 개발자", null, null, null,
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
     * 잠근 뒤에 역량 상태를 읽어야 Worker 가 막 저장한 결과를 본다.
     * 잠그기 전에 읽으면 저장 전 값(PROCESSING)을 보고 분석 중이라며 거절한다.
     */
    @Test
    void Worker_가_마지막_역량을_저장하는_중에_전체를_다시_분석하면_저장이_끝난_뒤의_상태를_보고_되돌린다() throws Exception {
        AdminJobPostingReanalysisResponse response = runWhileWorkerSaving(() -> reanalyze(ReanalysisScope.ALL));

        assertThat(response.reanalyzedRequirementCount()).isEqualTo(1);
        assertThat(requirementState(requirementId))
                .containsEntry("status", "PENDING")
                .containsEntry("attempt_count", 0);
    }

    /**
     * 관리자 작업은 Worker 가 역량 저장을 마칠 때까지 기다렸다가 진행하고, 서로 기다리다 멈추지 않는다.
     * 아직 분석 중인 역량이 있어 거절하며, Worker 가 저장한 결과와 분석 중인 역량은 그대로 둔다.
     */
    @Test
    void Worker_가_결과를_저장하는_중에_전체를_다시_분석하면_기다렸다가_아직_분석_중인_역량을_보고_거절한다() throws Exception {
        long inProgressId = insertRequirement(2, "PROCESSING", 1, null);

        Throwable thrown = runWhileWorkerSaving(() -> catchThrowable(() -> reanalyze(ReanalysisScope.ALL)));

        assertThat(thrown).isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_ANALYSIS_IN_PROGRESS);
        assertThat(requirementState(requirementId)).containsEntry("status", "READY");
        assertThat(requirementState(inProgressId))
                .containsEntry("status", "PROCESSING")
                .containsEntry("attempt_count", 1);
    }

    @Test
    void Worker_가_다른_역량을_저장하는_중에_실패한_역량을_다시_분석하면_기다렸다가_실패한_역량만_되돌린다() throws Exception {
        jdbcTemplate.update("UPDATE job_postings SET status = 'FAILED' WHERE id = ?", postingId);
        long failedId = insertRequirement(2, "FAILED", 3, "ATTEMPTS_EXHAUSTED");

        AdminJobPostingReanalysisResponse response = runWhileWorkerSaving(() -> reanalyze(ReanalysisScope.FAILED));

        assertThat(response.reanalyzedRequirementCount()).isEqualTo(1);
        assertThat(requirementState(requirementId)).containsEntry("status", "READY");
        assertThat(requirementState(failedId))
                .containsEntry("status", "PENDING")
                .containsEntry("attempt_count", 0)
                .containsEntry("last_error", null);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM job_postings WHERE id = ?", String.class, postingId))
                .isEqualTo("PENDING");
    }

    private AdminJobPostingReanalysisResponse reanalyze(ReanalysisScope scope) {
        return jobPostingAdminService.reanalyze(postingId, UserRole.ADMIN, new AdminJobPostingReanalysisRequest(scope));
    }

    private long insertRequirement(int displayOrder, String status, int attemptCount, String lastError) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO job_requirements (job_posting_id, display_order, name, category, summary, status,
                                              attempt_count, last_error, created_at, updated_at)
                VALUES (?, ?, 'Kafka', 'PREFERRED', '요약', ?, ?, ?, now(), now())
                RETURNING id
                """, Long.class, postingId, displayOrder, status, attemptCount, lastError);
    }

    private Map<String, Object> requirementState(long id) {
        return jdbcTemplate.queryForMap(
                "SELECT status, attempt_count, last_error FROM job_requirements WHERE id = ?", id);
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

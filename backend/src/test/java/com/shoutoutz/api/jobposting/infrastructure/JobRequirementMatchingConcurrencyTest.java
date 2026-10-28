package com.shoutoutz.api.jobposting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import com.shoutoutz.api.jobposting.application.JobRequirementMatchingRepository;
import com.shoutoutz.api.jobposting.application.dto.ClaimedJobRequirement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

/**
 * 서버 여러 대가 동시에 가져가도 같은 역량을 두 번 가져가지 않는지 확인한다.
 * 스레드마다 따로 커밋해야 잠금이 실제로 부딪치므로 테스트 트랜잭션을 쓰지 않고 직접 지운다.
 */
@ActiveProfiles("test")
@SpringBootTest
class JobRequirementMatchingConcurrencyTest {

    private static final int REQUIREMENT_COUNT = 6;
    private static final int THREAD_COUNT = 8;
    private static final Instant NOW = Instant.parse("2026-10-28T01:00:00Z");

    @Autowired
    private JobRequirementMatchingRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long adminId;
    private long postingId;

    @BeforeEach
    void setUp() {
        // 로컬 DB에 가져갈 역량이 남아 있으면 그 역량을 건드리게 되므로 검증하지 않는다.
        Integer queued = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM job_requirements WHERE status IN ('PENDING', 'PROCESSING')", Integer.class);
        assumeThat(queued).isZero();

        adminId = jdbcTemplate.queryForObject("INSERT INTO users (handle) VALUES (?) RETURNING id", Long.class,
                "@m" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        postingId = jdbcTemplate.queryForObject("""
                INSERT INTO job_postings (source_url, content_hash, company_name, job_role, title, status,
                                          registered_by, created_at, updated_at)
                VALUES (?, repeat('a', 64), '우테코', 'BACKEND', '백엔드 개발자', 'PENDING', ?, now(), now())
                RETURNING id
                """, Long.class, "https://example.com/jobs/" + UUID.randomUUID(), adminId);
        for (int order = 1; order <= REQUIREMENT_COUNT; order++) {
            jdbcTemplate.update("""
                    INSERT INTO job_requirements (job_posting_id, display_order, name, category, summary, status,
                                                  created_at, updated_at)
                    VALUES (?, ?, 'Spring', 'REQUIRED', '요약', 'PENDING', now(), now())
                    """, postingId, order);
        }
    }

    @AfterEach
    void cleanUp() {
        if (postingId != 0) {
            jdbcTemplate.update("DELETE FROM job_postings WHERE id = ?", postingId);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", adminId);
        }
    }

    @Test
    void 동시에_가져가도_같은_역량을_두_번_가져가지_않는다() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Optional<ClaimedJobRequirement>>> tasks = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            tasks.add(() -> {
                start.await();
                return repository.claimNext(NOW, NOW.plusSeconds(180));
            });
        }

        List<Long> claimedIds = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        try {
            List<Future<Optional<ClaimedJobRequirement>>> futures = new ArrayList<>();
            for (Callable<Optional<ClaimedJobRequirement>> task : tasks) {
                futures.add(executor.submit(task));
            }
            start.countDown();
            for (Future<Optional<ClaimedJobRequirement>> future : futures) {
                future.get(10, TimeUnit.SECONDS).ifPresent(claimed -> claimedIds.add(claimed.requirementId()));
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(claimedIds).doesNotHaveDuplicates().hasSize(REQUIREMENT_COUNT);
        Integer attempts = jdbcTemplate.queryForObject(
                "SELECT sum(attempt_count) FROM job_requirements WHERE job_posting_id = ?", Integer.class, postingId);
        assertThat(attempts).isEqualTo(REQUIREMENT_COUNT);
    }
}

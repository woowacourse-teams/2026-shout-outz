package com.shoutoutz.api.jobposting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

import com.shoutoutz.api.jobposting.application.JobRequirementMatchingRepository;
import com.shoutoutz.api.jobposting.application.dto.ClaimedJobRequirement;
import com.shoutoutz.api.jobposting.application.dto.RequirementRecommendation;
import com.shoutoutz.api.jobposting.domain.RecommendationVerdict;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 서버 여러 대가 동시에 처리할 때와, 완료 저장이 한 트랜잭션으로 묶이는지 확인한다.
 * 실제로 커밋·롤백돼야 잠금이 부딪치고 되돌려지는 게 보이므로 테스트 트랜잭션을 쓰지 않고 직접 지운다.
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

    @Test
    void 역량이_동시에_끝나도_공고를_완료로_바꾼다() throws Exception {
        List<ClaimedJobRequirement> claimed = new ArrayList<>();
        for (int i = 0; i < REQUIREMENT_COUNT; i++) {
            claimed.add(repository.claimNext(NOW, NOW.plusSeconds(180)).orElseThrow());
        }
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(REQUIREMENT_COUNT);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (ClaimedJobRequirement requirement : claimed) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return repository.complete(requirement, List.of(), NOW);
                }));
            }
            start.countDown();
            for (Future<Boolean> future : futures) {
                assertThat(future.get(10, TimeUnit.SECONDS)).isTrue();
            }
        } finally {
            executor.shutdownNow();
        }

        String status = jdbcTemplate.queryForObject("SELECT status FROM job_postings WHERE id = ?", String.class,
                postingId);
        assertThat(status).isEqualTo("READY");
    }

    @Test
    void 완료_저장_중에_실패하면_역량과_이전_판정_결과를_그대로_둔다() {
        ClaimedJobRequirement claimed = repository.claimNext(NOW, NOW.plusSeconds(180)).orElseThrow();
        jdbcTemplate.update("""
                INSERT INTO requirement_recommendations (job_requirement_id, resource_source, resource_external_key,
                                                         verdict, reason, evidence, similarity_score, created_at)
                VALUES (?, 'GITHUB_PR', 'kept', 'MATCH', '이전 판정', '[]'::jsonb, 0.5, now())
                """, claimed.requirementId());
        RequirementRecommendation duplicated = new RequirementRecommendation("GITHUB_PR", "pr-1",
                RecommendationVerdict.MATCH, "이유", "설명", 0.8, List.of());

        assertThatThrownBy(() -> repository.complete(claimed, List.of(duplicated, duplicated), NOW))
                .isInstanceOf(DuplicateKeyException.class);

        String status = jdbcTemplate.queryForObject("SELECT status FROM job_requirements WHERE id = ?",
                String.class, claimed.requirementId());
        assertThat(status).isEqualTo("PROCESSING");
        assertThat(jdbcTemplate.queryForList(
                "SELECT resource_external_key FROM requirement_recommendations WHERE job_requirement_id = ?",
                String.class, claimed.requirementId())).containsExactly("kept");
    }
}

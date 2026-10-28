package com.shoutoutz.api.jobposting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.jobposting.application.JobRequirementMatchingRepository;
import com.shoutoutz.api.jobposting.application.dto.ClaimedJobRequirement;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.MatchingFailure;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class JobRequirementMatchingRepositoryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-28T01:00:00Z");
    private static final Instant LEASE_UNTIL = NOW.plus(Duration.ofSeconds(180));

    @Autowired
    private JobRequirementMatchingRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long adminId;

    @BeforeEach
    void setUp() {
        // 로컬 DB에 남아 있는 다른 역량을 가져가지 않도록, 이 테스트 트랜잭션 안에서만 끝난 것으로 둔다.
        jdbcTemplate.update("UPDATE job_requirements SET status = 'READY' WHERE status IN ('PENDING', 'PROCESSING')");
        adminId = jdbcTemplate.queryForObject("INSERT INTO users (handle) VALUES (?) RETURNING id", Long.class,
                "@m" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
    }

    @Test
    void 대기_역량을_가져가며_시도_횟수를_올리고_공고를_분석_중으로_바꾼다() {
        long postingId = insertPosting("PENDING");
        long requirementId = insertRequirement(postingId, 1, "PENDING");

        ClaimedJobRequirement claimed = repository.claimNext(NOW, LEASE_UNTIL).orElseThrow();

        assertThat(claimed).isEqualTo(new ClaimedJobRequirement(requirementId, postingId, 1, JobRole.BACKEND,
                "Spring", RequirementCategory.REQUIRED, "Spring 기반 서비스를 개발한 경험을 요구해요."));
        Map<String, Object> requirement = requirement(requirementId);
        assertThat(requirement.get("status")).isEqualTo("PROCESSING");
        assertThat(requirement.get("lease_until")).isEqualTo(Timestamp.from(LEASE_UNTIL));
        assertThat(postingStatus(postingId)).isEqualTo("PROCESSING");
    }

    @Test
    void 가져갈_역량이_없으면_비어_있다() {
        long postingId = insertPosting("READY");
        insertRequirement(postingId, 1, "READY");

        assertThat(repository.claimNext(NOW, LEASE_UNTIL)).isEmpty();
    }

    @Test
    void 먼저_등록된_역량부터_가져간다() {
        long postingId = insertPosting("PENDING");
        long first = insertRequirement(postingId, 1, "PENDING");
        insertRequirement(postingId, 2, "PENDING");

        assertThat(repository.claimNext(NOW, LEASE_UNTIL).orElseThrow().requirementId()).isEqualTo(first);
    }

    @Test
    void 다시_시도할_시각이_되기_전에는_가져가지_않는다() {
        long postingId = insertPosting("PROCESSING");
        long requirementId = insertRequirement(postingId, 1, "PENDING");
        jdbcTemplate.update("UPDATE job_requirements SET next_attempt_at = ? WHERE id = ?",
                Timestamp.from(NOW.plusSeconds(1)), requirementId);

        assertThat(repository.claimNext(NOW, LEASE_UNTIL)).isEmpty();
        assertThat(repository.claimNext(NOW.plusSeconds(1), LEASE_UNTIL)).isPresent();
        assertThat(requirement(requirementId).get("next_attempt_at")).isNull();
    }

    @Test
    void 실패로_끝난_역량은_가져가지_않는다() {
        long postingId = insertPosting("FAILED");
        insertRequirement(postingId, 1, "FAILED");

        assertThat(repository.claimNext(NOW, LEASE_UNTIL)).isEmpty();
    }

    @Test
    void 작업_기한이_지난_분석_중_역량을_다시_가져간다() {
        long postingId = insertPosting("PROCESSING");
        long requirementId = insertRequirement(postingId, 1, "PROCESSING");
        jdbcTemplate.update("UPDATE job_requirements SET attempt_count = 1, lease_until = ? WHERE id = ?",
                Timestamp.from(NOW.minusSeconds(1)), requirementId);

        ClaimedJobRequirement claimed = repository.claimNext(NOW, LEASE_UNTIL).orElseThrow();

        assertThat(claimed.requirementId()).isEqualTo(requirementId);
        assertThat(claimed.attemptCount()).isEqualTo(2);
    }

    @Test
    void 작업_기한이_남은_분석_중_역량은_가져가지_않는다() {
        long postingId = insertPosting("PROCESSING");
        long requirementId = insertRequirement(postingId, 1, "PROCESSING");
        jdbcTemplate.update("UPDATE job_requirements SET attempt_count = 1, lease_until = ? WHERE id = ?",
                Timestamp.from(NOW), requirementId);

        assertThat(repository.claimNext(NOW, LEASE_UNTIL)).isEmpty();
    }

    @Test
    void 실패한_공고의_남은_역량은_계속_가져가고_공고는_실패로_둔다() {
        long postingId = insertPosting("FAILED");
        long requirementId = insertRequirement(postingId, 2, "PENDING");

        assertThat(repository.claimNext(NOW, LEASE_UNTIL).orElseThrow().requirementId()).isEqualTo(requirementId);
        assertThat(postingStatus(postingId)).isEqualTo("FAILED");
    }

    @Test
    void 재시도를_예약하면_역량을_대기로_되돌리고_실패_이유를_남긴다() {
        long postingId = insertPosting("PENDING");
        long requirementId = insertRequirement(postingId, 1, "PENDING");
        ClaimedJobRequirement claimed = repository.claimNext(NOW, LEASE_UNTIL).orElseThrow();
        Instant nextAttemptAt = NOW.plus(Duration.ofMinutes(1));

        boolean updated = repository.scheduleRetry(claimed, MatchingFailure.SEARCH_FAILED, nextAttemptAt, NOW);

        assertThat(updated).isTrue();
        Map<String, Object> requirement = requirement(requirementId);
        assertThat(requirement.get("status")).isEqualTo("PENDING");
        assertThat(requirement.get("next_attempt_at")).isEqualTo(Timestamp.from(nextAttemptAt));
        assertThat(requirement.get("lease_until")).isNull();
        assertThat(requirement.get("last_error")).isEqualTo("SEARCH_FAILED");
        assertThat(requirement.get("attempt_count")).isEqualTo(1);
        assertThat(postingStatus(postingId)).isEqualTo("PROCESSING");
    }

    @Test
    void 실패로_끝내면_역량과_공고를_실패로_바꾼다() {
        long postingId = insertPosting("PENDING");
        long requirementId = insertRequirement(postingId, 1, "PENDING");
        ClaimedJobRequirement claimed = repository.claimNext(NOW, LEASE_UNTIL).orElseThrow();

        boolean updated = repository.markFailed(claimed, MatchingFailure.INVALID_QUERY, NOW);

        assertThat(updated).isTrue();
        Map<String, Object> requirement = requirement(requirementId);
        assertThat(requirement.get("status")).isEqualTo("FAILED");
        assertThat(requirement.get("lease_until")).isNull();
        assertThat(requirement.get("last_error")).isEqualTo("INVALID_QUERY");
        assertThat(postingStatus(postingId)).isEqualTo("FAILED");
    }

    @Test
    void 역량_하나가_실패해도_같은_공고의_다른_역량은_계속_가져간다() {
        long postingId = insertPosting("PENDING");
        insertRequirement(postingId, 1, "PENDING");
        long other = insertRequirement(postingId, 2, "PENDING");
        ClaimedJobRequirement failed = repository.claimNext(NOW, LEASE_UNTIL).orElseThrow();

        repository.markFailed(failed, MatchingFailure.INVALID_QUERY, NOW);

        assertThat(requirement(other).get("status")).isEqualTo("PENDING");
        assertThat(repository.claimNext(NOW, LEASE_UNTIL).orElseThrow().requirementId()).isEqualTo(other);
        assertThat(postingStatus(postingId)).isEqualTo("FAILED");
    }

    @Test
    void 이미_실패한_공고에서_다른_역량이_실패해도_역량을_실패로_바꾸고_공고는_실패로_둔다() {
        long postingId = insertPosting("FAILED");
        long requirementId = insertRequirement(postingId, 2, "PENDING");
        ClaimedJobRequirement claimed = repository.claimNext(NOW, LEASE_UNTIL).orElseThrow();

        boolean updated = repository.markFailed(claimed, MatchingFailure.UNEXPECTED, NOW);

        assertThat(updated).isTrue();
        assertThat(requirement(requirementId).get("status")).isEqualTo("FAILED");
        assertThat(postingStatus(postingId)).isEqualTo("FAILED");
    }

    @Test
    void 다른_Worker가_다시_가져간_역량에는_늦게_끝난_결과를_반영하지_않는다() {
        long postingId = insertPosting("PENDING");
        long requirementId = insertRequirement(postingId, 1, "PENDING");
        ClaimedJobRequirement late = repository.claimNext(NOW, LEASE_UNTIL).orElseThrow();
        ClaimedJobRequirement current = repository.claimNext(LEASE_UNTIL.plusSeconds(1),
                LEASE_UNTIL.plusSeconds(181)).orElseThrow();

        boolean retried = repository.scheduleRetry(late, MatchingFailure.JUDGE_FAILED, NOW, NOW);
        boolean failed = repository.markFailed(late, MatchingFailure.UNEXPECTED, NOW);

        assertThat(current.attemptCount()).isEqualTo(2);
        assertThat(retried).isFalse();
        assertThat(failed).isFalse();
        Map<String, Object> requirement = requirement(requirementId);
        assertThat(requirement.get("status")).isEqualTo("PROCESSING");
        assertThat(requirement.get("last_error")).isNull();
        assertThat(postingStatus(postingId)).isEqualTo("PROCESSING");
    }

    private long insertPosting(String status) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO job_postings (source_url, content_hash, company_name, job_role, title, status,
                                          registered_by, created_at, updated_at)
                VALUES (?, repeat('a', 64), '우테코', 'BACKEND', '백엔드 개발자', ?, ?, now(), now())
                RETURNING id
                """, Long.class, "https://example.com/jobs/" + UUID.randomUUID(), status, adminId);
    }

    private long insertRequirement(long postingId, int displayOrder, String status) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO job_requirements (job_posting_id, display_order, name, category, summary, status,
                                              created_at, updated_at)
                VALUES (?, ?, 'Spring', 'REQUIRED', 'Spring 기반 서비스를 개발한 경험을 요구해요.', ?, now(), now())
                RETURNING id
                """, Long.class, postingId, displayOrder, status);
    }

    private Map<String, Object> requirement(long requirementId) {
        return jdbcTemplate.queryForMap("""
                SELECT status, attempt_count, lease_until, next_attempt_at, last_error
                FROM job_requirements WHERE id = ?
                """, requirementId);
    }

    private String postingStatus(long postingId) {
        return jdbcTemplate.queryForObject("SELECT status FROM job_postings WHERE id = ?", String.class, postingId);
    }
}

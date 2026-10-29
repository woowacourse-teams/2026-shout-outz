package com.shoutoutz.api.jobposting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.jobposting.application.JobRequirementMatchingRepository;
import com.shoutoutz.api.jobposting.application.dto.CandidateChunk;
import com.shoutoutz.api.jobposting.application.dto.CandidateChunk.ChunkKind;
import com.shoutoutz.api.jobposting.application.dto.ClaimedJobRequirement;
import com.shoutoutz.api.jobposting.application.dto.RequirementRecommendation;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.MatchingFailure;
import com.shoutoutz.api.jobposting.domain.RecommendationVerdict;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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

    @Test
    void 완료하면_판정_결과와_판정_근거를_저장하고_역량을_완료로_바꾼다() {
        long postingId = insertPosting("PENDING");
        long requirementId = insertRequirement(postingId, 1, "PENDING");
        ClaimedJobRequirement first = repository.claimNext(NOW, LEASE_UNTIL).orElseThrow();
        repository.scheduleRetry(first, MatchingFailure.SEARCH_FAILED, NOW, NOW);
        ClaimedJobRequirement claimed = repository.claimNext(NOW, LEASE_UNTIL).orElseThrow();

        boolean completed = repository.complete(claimed, List.of(recommendation("pr-1", RecommendationVerdict.MATCH)),
                NOW);

        assertThat(completed).isTrue();
        Map<String, Object> requirement = requirement(requirementId);
        assertThat(requirement.get("status")).isEqualTo("READY");
        assertThat(requirement.get("lease_until")).isNull();
        assertThat(requirement.get("last_error")).isNull();
        Map<String, Object> saved = jdbcTemplate.queryForMap("""
                SELECT resource_source, resource_external_key, verdict, reason, description, similarity_score,
                       jsonb_array_length(evidence) AS evidence_count,
                       evidence -> 0 ->> 'kind' AS first_kind, evidence -> 0 ->> 'text' AS first_text,
                       (evidence -> 0 ->> 'similarityScore')::float8 AS first_score
                FROM requirement_recommendations WHERE job_requirement_id = ?
                """, requirementId);
        assertThat(saved).containsEntry("resource_source", "GITHUB_PR")
                .containsEntry("resource_external_key", "pr-1")
                .containsEntry("verdict", "MATCH")
                .containsEntry("reason", "Spring 으로 API 를 만든 경험이 있어요.")
                .containsEntry("description", "Spring 으로 API 를 만든 PR 이에요.")
                .containsEntry("similarity_score", 0.8)
                .containsEntry("evidence_count", 2)
                .containsEntry("first_kind", "EXPERIENCE")
                .containsEntry("first_text", "Spring 으로 주문 API 를 만들었다.")
                .containsEntry("first_score", 0.8);
    }

    /** 완료된 역량을 다시 분석할 때처럼 이전 판정 결과가 남아 있어도 한 벌만 남는다. */
    @Test
    void 완료하면_이전에_저장한_판정_결과는_지우고_새로_넣는다() {
        long postingId = insertPosting("PENDING");
        long requirementId = insertRequirement(postingId, 1, "PENDING");
        jdbcTemplate.update("""
                INSERT INTO requirement_recommendations (job_requirement_id, resource_source, resource_external_key,
                                                         verdict, reason, evidence, similarity_score, created_at)
                VALUES (?, 'GITHUB_PR', 'old', 'MATCH', '이전 판정', '[]'::jsonb, 0.5, now())
                """, requirementId);
        ClaimedJobRequirement claimed = repository.claimNext(NOW, LEASE_UNTIL).orElseThrow();

        repository.complete(claimed, List.of(recommendation("pr-1", RecommendationVerdict.MATCH),
                recommendation("pr-2", RecommendationVerdict.NOT_MATCH)), NOW);

        assertThat(jdbcTemplate.queryForList("""
                SELECT resource_external_key FROM requirement_recommendations
                WHERE job_requirement_id = ? ORDER BY resource_external_key
                """, String.class, requirementId)).containsExactly("pr-1", "pr-2");
    }

    @Test
    void 검색된_자료가_없으면_판정_결과_없이_완료한다() {
        long postingId = insertPosting("PENDING");
        long requirementId = insertRequirement(postingId, 1, "PENDING");
        ClaimedJobRequirement claimed = repository.claimNext(NOW, LEASE_UNTIL).orElseThrow();

        assertThat(repository.complete(claimed, List.of(), NOW)).isTrue();

        assertThat(requirement(requirementId).get("status")).isEqualTo("READY");
        assertThat(recommendationCount(requirementId)).isZero();
        assertThat(postingStatus(postingId)).isEqualTo("READY");
    }

    @Test
    void 남은_역량이_있으면_공고를_분석_중으로_두고_마지막_역량이_끝나면_완료로_바꾼다() {
        long postingId = insertPosting("PENDING");
        insertRequirement(postingId, 1, "PENDING");
        insertRequirement(postingId, 2, "PENDING");
        ClaimedJobRequirement first = repository.claimNext(NOW, LEASE_UNTIL).orElseThrow();
        ClaimedJobRequirement second = repository.claimNext(NOW, LEASE_UNTIL).orElseThrow();

        repository.complete(first, List.of(), NOW);
        assertThat(postingStatus(postingId)).isEqualTo("PROCESSING");

        repository.complete(second, List.of(), NOW);
        assertThat(postingStatus(postingId)).isEqualTo("READY");
    }

    @Test
    void 실패한_공고는_역량이_모두_끝나도_실패로_둔다() {
        long postingId = insertPosting("PENDING");
        insertRequirement(postingId, 1, "PENDING");
        insertRequirement(postingId, 2, "PENDING");
        ClaimedJobRequirement failed = repository.claimNext(NOW, LEASE_UNTIL).orElseThrow();
        ClaimedJobRequirement other = repository.claimNext(NOW, LEASE_UNTIL).orElseThrow();
        repository.markFailed(failed, MatchingFailure.INVALID_QUERY, NOW);

        assertThat(repository.complete(other, List.of(), NOW)).isTrue();

        assertThat(postingStatus(postingId)).isEqualTo("FAILED");
    }

    @Test
    void 다른_Worker가_다시_가져간_역량에는_늦게_끝난_판정_결과를_저장하지_않는다() {
        long postingId = insertPosting("PENDING");
        long requirementId = insertRequirement(postingId, 1, "PENDING");
        jdbcTemplate.update("""
                INSERT INTO requirement_recommendations (job_requirement_id, resource_source, resource_external_key,
                                                         verdict, reason, evidence, similarity_score, created_at)
                VALUES (?, 'GITHUB_PR', 'kept', 'MATCH', '이전 판정', '[]'::jsonb, 0.5, now())
                """, requirementId);
        ClaimedJobRequirement late = repository.claimNext(NOW, LEASE_UNTIL).orElseThrow();
        repository.claimNext(LEASE_UNTIL.plusSeconds(1), LEASE_UNTIL.plusSeconds(181)).orElseThrow();

        boolean completed = repository.complete(late, List.of(recommendation("pr-1", RecommendationVerdict.MATCH)),
                NOW);

        assertThat(completed).isFalse();
        assertThat(requirement(requirementId).get("status")).isEqualTo("PROCESSING");
        assertThat(jdbcTemplate.queryForList(
                "SELECT resource_external_key FROM requirement_recommendations WHERE job_requirement_id = ?",
                String.class, requirementId)).containsExactly("kept");
        assertThat(postingStatus(postingId)).isEqualTo("PROCESSING");
    }

    private RequirementRecommendation recommendation(String externalKey, RecommendationVerdict verdict) {
        return new RequirementRecommendation("GITHUB_PR", externalKey, verdict, "Spring 으로 API 를 만든 경험이 있어요.",
                verdict == RecommendationVerdict.NOT_MATCH ? null : "Spring 으로 API 를 만든 PR 이에요.", 0.8,
                List.of(new CandidateChunk(ChunkKind.EXPERIENCE, "Spring 으로 주문 API 를 만들었다.", 0.8),
                        new CandidateChunk(ChunkKind.INTRO, "주문 API 를 소개한다.", 0.6)));
    }

    private int recommendationCount(long requirementId) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM requirement_recommendations WHERE job_requirement_id = ?",
                Integer.class, requirementId);
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

package com.shoutoutz.api.jobposting.infrastructure;

import com.shoutoutz.api.jobposting.application.JobRequirementMatchingRepository;
import com.shoutoutz.api.jobposting.application.dto.ClaimedJobRequirement;
import com.shoutoutz.api.jobposting.application.dto.RequirementRecommendation;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.MatchingFailure;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class JobRequirementMatchingRepositoryImpl implements JobRequirementMatchingRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /** 역량 선점과 공고 상태 변경을 한 SQL 문으로 해서, 둘 중 하나만 반영되는 일이 없게 한다. */
    @Override
    public Optional<ClaimedJobRequirement> claimNext(Instant now, Instant leaseUntil) {
        List<ClaimedJobRequirement> claimed = jdbcTemplate.query("""
                WITH claimed AS (
                    UPDATE job_requirements r
                    SET status = 'PROCESSING',
                        attempt_count = r.attempt_count + 1,
                        lease_until = :leaseUntil,
                        next_attempt_at = NULL,
                        updated_at = :now
                    WHERE r.id = (
                        SELECT candidate.id
                        FROM job_requirements candidate
                        WHERE candidate.status IN ('PENDING', 'PROCESSING')
                          AND (
                              (candidate.status = 'PENDING'
                                  AND (candidate.next_attempt_at IS NULL OR candidate.next_attempt_at <= :now))
                              OR (candidate.status = 'PROCESSING' AND candidate.lease_until < :now)
                          )
                        ORDER BY candidate.id
                        LIMIT 1
                        FOR UPDATE SKIP LOCKED
                    )
                    RETURNING r.id, r.job_posting_id, r.attempt_count, r.name, r.category, r.summary
                ),
                started_posting AS (
                    UPDATE job_postings p
                    SET status = 'PROCESSING', updated_at = :now
                    FROM claimed c
                    WHERE p.id = c.job_posting_id AND p.status = 'PENDING'
                )
                SELECT c.id, c.job_posting_id, c.attempt_count, c.name, c.category, c.summary, p.job_role
                FROM claimed c
                JOIN job_postings p ON p.id = c.job_posting_id
                """, new MapSqlParameterSource()
                        .addValue("now", Timestamp.from(now))
                        .addValue("leaseUntil", Timestamp.from(leaseUntil)),
                (rs, rowNum) -> new ClaimedJobRequirement(
                        rs.getLong("id"),
                        rs.getLong("job_posting_id"),
                        rs.getInt("attempt_count"),
                        JobRole.valueOf(rs.getString("job_role")),
                        rs.getString("name"),
                        RequirementCategory.valueOf(rs.getString("category")),
                        rs.getString("summary")));
        return claimed.stream().findFirst();
    }

    @Override
    public boolean scheduleRetry(ClaimedJobRequirement requirement, MatchingFailure failure, Instant nextAttemptAt,
                                 Instant now) {
        int updated = jdbcTemplate.update("""
                UPDATE job_requirements
                SET status = 'PENDING', lease_until = NULL, next_attempt_at = :nextAttemptAt,
                    last_error = :lastError, updated_at = :now
                WHERE id = :id AND status = 'PROCESSING' AND attempt_count = :attemptCount
                """, ownedBy(requirement)
                        .addValue("nextAttemptAt", Timestamp.from(nextAttemptAt))
                        .addValue("lastError", failure.name())
                        .addValue("now", Timestamp.from(now)));
        return updated == 1;
    }

    /** 역량 실패와 공고 실패를 한 SQL 문으로 해서, 역량만 실패하고 공고가 남는 일이 없게 한다. */
    @Override
    public boolean markFailed(ClaimedJobRequirement requirement, MatchingFailure failure, Instant now) {
        Integer failed = jdbcTemplate.queryForObject("""
                WITH failed AS (
                    UPDATE job_requirements
                    SET status = 'FAILED', lease_until = NULL, next_attempt_at = NULL,
                        last_error = :lastError, updated_at = :now
                    WHERE id = :id AND status = 'PROCESSING' AND attempt_count = :attemptCount
                    RETURNING job_posting_id
                ),
                failed_posting AS (
                    UPDATE job_postings p
                    SET status = 'FAILED', updated_at = :now
                    FROM failed f
                    WHERE p.id = f.job_posting_id AND p.status IN ('PENDING', 'PROCESSING')
                )
                SELECT count(*) FROM failed
                """, ownedBy(requirement)
                        .addValue("lastError", failure.name())
                        .addValue("now", Timestamp.from(now)),
                Integer.class);
        return failed != null && failed == 1;
    }

    /**
     * 역량 행을 먼저 잠그고 공고 행을 잠근다 (가져오기, 실패 저장과 같은 순서라 서로 기다리다 멈추지 않는다).
     * 마지막 역량 두 개가 동시에 끝나면 서로의 완료가 아직 커밋 전이라 둘 다 공고를 완료로 못 바꿀 수 있어서,
     * 공고 행을 잠근 뒤 다음 문장에서 남은 역량을 센다. 먼저 끝난 쪽이 커밋해야 잠금이 풀려 그 완료가 보인다.
     */
    @Override
    @Transactional
    public boolean complete(ClaimedJobRequirement requirement, List<RequirementRecommendation> recommendations,
                            Instant now) {
        int updated = jdbcTemplate.update("""
                UPDATE job_requirements
                SET status = 'READY', lease_until = NULL, next_attempt_at = NULL, last_error = NULL, updated_at = :now
                WHERE id = :id AND status = 'PROCESSING' AND attempt_count = :attemptCount
                """, ownedBy(requirement).addValue("now", Timestamp.from(now)));
        if (updated == 0) {
            return false;
        }

        jdbcTemplate.update("DELETE FROM requirement_recommendations WHERE job_requirement_id = :id",
                ownedBy(requirement));
        jdbcTemplate.batchUpdate("""
                INSERT INTO requirement_recommendations (job_requirement_id, resource_source, resource_external_key,
                                                         verdict, reason, description, evidence, similarity_score,
                                                         created_at)
                VALUES (:requirementId, :source, :externalKey, :verdict, :reason, :description,
                        CAST(:evidence AS jsonb), :similarityScore, :now)
                """, recommendations.stream()
                .map(recommendation -> new MapSqlParameterSource()
                        .addValue("requirementId", requirement.requirementId())
                        .addValue("source", recommendation.source())
                        .addValue("externalKey", recommendation.externalKey())
                        .addValue("verdict", recommendation.verdict().name())
                        .addValue("reason", recommendation.reason())
                        .addValue("description", recommendation.description())
                        .addValue("evidence", objectMapper.writeValueAsString(recommendation.evidence()))
                        .addValue("similarityScore", recommendation.similarityScore())
                        .addValue("now", Timestamp.from(now)))
                .toArray(SqlParameterSource[]::new));

        MapSqlParameterSource posting = new MapSqlParameterSource()
                .addValue("postingId", requirement.jobPostingId())
                .addValue("now", Timestamp.from(now));
        jdbcTemplate.queryForObject("SELECT id FROM job_postings WHERE id = :postingId FOR UPDATE", posting,
                Long.class);
        jdbcTemplate.update("""
                UPDATE job_postings p
                SET status = 'READY', updated_at = :now
                WHERE p.id = :postingId AND p.status = 'PROCESSING'
                  AND NOT EXISTS (
                      SELECT 1 FROM job_requirements r WHERE r.job_posting_id = p.id AND r.status <> 'READY'
                  )
                """, posting);
        return true;
    }

    private MapSqlParameterSource ownedBy(ClaimedJobRequirement requirement) {
        return new MapSqlParameterSource()
                .addValue("id", requirement.requirementId())
                .addValue("attemptCount", requirement.attemptCount());
    }
}

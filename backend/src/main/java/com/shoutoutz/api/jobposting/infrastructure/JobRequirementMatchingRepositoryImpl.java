package com.shoutoutz.api.jobposting.infrastructure;

import com.shoutoutz.api.jobposting.application.JobRequirementMatchingRepository;
import com.shoutoutz.api.jobposting.application.dto.ClaimedJobRequirement;
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
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JobRequirementMatchingRepositoryImpl implements JobRequirementMatchingRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

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

    private MapSqlParameterSource ownedBy(ClaimedJobRequirement requirement) {
        return new MapSqlParameterSource()
                .addValue("id", requirement.requirementId())
                .addValue("attemptCount", requirement.attemptCount());
    }
}

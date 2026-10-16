package com.shoutoutz.api.feed.recommendation.infrastructure;

import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationRepository;
import com.shoutoutz.api.feed.recommendation.application.dto.GeneratedQuestion;
import com.shoutoutz.api.feed.recommendation.application.dto.QuestionRecommendationItem;
import com.shoutoutz.api.feed.recommendation.application.dto.RelatedQuestionRecommendations;
import com.shoutoutz.api.feed.recommendation.domain.QuestionRecommendationStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class QuestionRecommendationRepositoryImpl implements QuestionRecommendationRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int requestGeneration(long feedId, String title, String content, UUID outboxId, Instant now) {
        int generation = jdbcTemplate.queryForObject(
                """
                        INSERT INTO question_recommendation_jobs (
                            feed_id, generation, status, source_title, source_content,
                            attempt_count, created_at, updated_at
                        )
                        VALUES (?, 1, 'PENDING', ?, ?, 0, ?, ?)
                        ON CONFLICT (feed_id) DO UPDATE SET
                            generation = question_recommendation_jobs.generation + 1,
                            status = 'PENDING',
                            source_title = EXCLUDED.source_title,
                            source_content = EXCLUDED.source_content,
                            attempt_count = 0,
                            lease_until = NULL,
                            last_error = NULL,
                            updated_at = EXCLUDED.updated_at
                        RETURNING generation
                        """,
                Integer.class,
                feedId,
                title,
                content,
                Timestamp.from(now),
                Timestamp.from(now)
        );
        jdbcTemplate.update("DELETE FROM question_recommendation_items WHERE feed_id = ?", feedId);
        jdbcTemplate.update(
                """
                        INSERT INTO question_recommendation_outbox (
                            id, feed_id, generation, status, attempt_count,
                            next_attempt_at, created_at, updated_at
                        )
                        VALUES (?, ?, ?, 'PENDING', 0, ?, ?, ?)
                        """,
                outboxId,
                feedId,
                generation,
                Timestamp.from(now),
                Timestamp.from(now),
                Timestamp.from(now)
        );
        return generation;
    }

    @Override
    public Optional<RelatedQuestionRecommendations> findByFeedId(long feedId) {
        List<RecommendationJob> jobs = jdbcTemplate.query(
                """
                        SELECT generation, status
                        FROM question_recommendation_jobs
                        WHERE feed_id = ?
                        """,
                (resultSet, rowNumber) -> new RecommendationJob(
                        resultSet.getInt("generation"),
                        QuestionRecommendationStatus.valueOf(resultSet.getString("status"))
                ),
                feedId
        );
        if (jobs.isEmpty()) {
            return Optional.empty();
        }

        RecommendationJob job = jobs.getFirst();
        List<QuestionRecommendationItem> items = jdbcTemplate.query(
                """
                        SELECT id, title, content
                        FROM question_recommendation_items
                        WHERE feed_id = ?
                          AND generation = ?
                        ORDER BY display_order
                        """,
                recommendationItemRowMapper(),
                feedId,
                job.generation()
        );
        return Optional.of(new RelatedQuestionRecommendations(job.status(), items));
    }

    @Override
    public List<QuestionRecommendationItem> findRandomItems(int limit) {
        if (limit < 1) {
            return List.of();
        }
        return jdbcTemplate.query(
                """
                        SELECT candidate.id, candidate.title, candidate.content
                        FROM (
                            SELECT DISTINCT ON (item.feed_id)
                                item.feed_id, item.id, item.title, item.content
                            FROM question_recommendation_items item
                            JOIN question_recommendation_jobs job
                              ON job.feed_id = item.feed_id
                             AND job.generation = item.generation
                            JOIN feeds feed ON feed.id = item.feed_id
                            WHERE job.status = 'READY'
                              AND feed.feed_type = 'QUESTION'
                              AND feed.deleted_at IS NULL
                            ORDER BY item.feed_id, random()
                        ) candidate
                        ORDER BY random()
                        LIMIT ?
                        """,
                recommendationItemRowMapper(),
                limit
        );
    }

    @Override
    public List<OutboxMessage> claimOutbox(Instant now, Instant leaseUntil, int limit) {
        if (limit < 1) {
            return List.of();
        }
        return jdbcTemplate.query(
                """
                        UPDATE question_recommendation_outbox outbox
                        SET status = 'PROCESSING',
                            attempt_count = outbox.attempt_count + 1,
                            lease_until = ?,
                            updated_at = ?
                        WHERE outbox.id IN (
                            SELECT candidate.id
                            FROM question_recommendation_outbox candidate
                            WHERE (candidate.status = 'PENDING' AND candidate.next_attempt_at <= ?)
                               OR (candidate.status = 'PROCESSING' AND candidate.lease_until <= ?)
                            ORDER BY candidate.created_at, candidate.id
                            FOR UPDATE SKIP LOCKED
                            LIMIT ?
                        )
                        RETURNING outbox.id, outbox.feed_id, outbox.generation, outbox.attempt_count
                        """,
                (resultSet, rowNumber) -> new OutboxMessage(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getLong("feed_id"),
                        resultSet.getInt("generation"),
                        resultSet.getInt("attempt_count")
                ),
                Timestamp.from(leaseUntil),
                Timestamp.from(now),
                Timestamp.from(now),
                Timestamp.from(now),
                limit
        );
    }

    @Override
    public void markOutboxSent(UUID outboxId, Instant now) {
        jdbcTemplate.update(
                """
                        UPDATE question_recommendation_outbox
                        SET status = 'SENT', lease_until = NULL, sent_at = ?, updated_at = ?, last_error = NULL
                        WHERE id = ? AND status = 'PROCESSING'
                        """,
                Timestamp.from(now),
                Timestamp.from(now),
                outboxId
        );
    }

    @Override
    @Transactional
    public void retryOutbox(
            OutboxMessage message,
            String errorCode,
            Instant nextAttemptAt,
            int maxAttempts,
            Instant now
    ) {
        boolean exhausted = message.attempts() >= maxAttempts;
        jdbcTemplate.update(
                """
                        UPDATE question_recommendation_outbox
                        SET status = ?, lease_until = NULL, next_attempt_at = ?, last_error = ?, updated_at = ?
                        WHERE id = ? AND status = 'PROCESSING'
                        """,
                exhausted ? "FAILED" : "PENDING",
                Timestamp.from(nextAttemptAt),
                errorCode,
                Timestamp.from(now),
                message.id()
        );
        if (exhausted) {
            jdbcTemplate.update(
                    """
                            UPDATE question_recommendation_jobs
                            SET status = 'FAILED', lease_until = NULL, last_error = ?, updated_at = ?
                            WHERE feed_id = ? AND generation = ? AND status = 'PENDING'
                            """,
                    errorCode,
                    Timestamp.from(now),
                    message.feedId(),
                    message.generation()
            );
        }
    }

    @Override
    public GenerationClaim claimGeneration(long feedId, int generation, Instant now, Instant leaseUntil) {
        List<GenerationTask> claimed = jdbcTemplate.query(
                """
                        UPDATE question_recommendation_jobs
                        SET status = 'PROCESSING', attempt_count = attempt_count + 1,
                            lease_until = ?, updated_at = ?, last_error = NULL
                        WHERE feed_id = ? AND generation = ?
                          AND (status = 'PENDING' OR (status = 'PROCESSING' AND lease_until <= ?))
                        RETURNING feed_id, generation, source_title, source_content, attempt_count
                        """,
                generationTaskRowMapper(),
                Timestamp.from(leaseUntil),
                Timestamp.from(now),
                feedId,
                generation,
                Timestamp.from(now)
        );
        if (!claimed.isEmpty()) {
            return new GenerationClaim(GenerationClaim.ClaimStatus.CLAIMED, claimed.getFirst());
        }

        List<CurrentJob> currentJobs = jdbcTemplate.query(
                """
                        SELECT generation, status
                        FROM question_recommendation_jobs
                        WHERE feed_id = ?
                        """,
                (resultSet, rowNumber) -> new CurrentJob(
                        resultSet.getInt("generation"),
                        QuestionRecommendationStatus.valueOf(resultSet.getString("status"))
                ),
                feedId
        );
        if (currentJobs.isEmpty() || currentJobs.getFirst().generation() != generation) {
            return new GenerationClaim(GenerationClaim.ClaimStatus.OBSOLETE, null);
        }
        QuestionRecommendationStatus status = currentJobs.getFirst().status();
        if (status == QuestionRecommendationStatus.READY || status == QuestionRecommendationStatus.FAILED) {
            return new GenerationClaim(GenerationClaim.ClaimStatus.OBSOLETE, null);
        }
        return new GenerationClaim(GenerationClaim.ClaimStatus.BUSY, null);
    }

    @Override
    @Transactional
    public boolean completeGeneration(
            long feedId,
            int generation,
            List<GeneratedQuestion> questions,
            Instant now
    ) {
        int updated = jdbcTemplate.update(
                """
                        UPDATE question_recommendation_jobs
                        SET status = 'READY', lease_until = NULL, last_error = NULL, updated_at = ?
                        WHERE feed_id = ? AND generation = ? AND status = 'PROCESSING'
                        """,
                Timestamp.from(now),
                feedId,
                generation
        );
        if (updated == 0) {
            return false;
        }
        jdbcTemplate.update("DELETE FROM question_recommendation_items WHERE feed_id = ?", feedId);
        for (int index = 0; index < questions.size(); index++) {
            GeneratedQuestion question = questions.get(index);
            jdbcTemplate.update(
                    """
                            INSERT INTO question_recommendation_items (
                                feed_id, generation, display_order, title, content, created_at
                            )
                            VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    feedId,
                    generation,
                    index + 1,
                    question.title(),
                    question.content(),
                    Timestamp.from(now)
            );
        }
        return true;
    }

    @Override
    public void retryGeneration(long feedId, int generation, String errorCode, Instant now) {
        jdbcTemplate.update(
                """
                        UPDATE question_recommendation_jobs
                        SET status = 'PENDING', lease_until = NULL, last_error = ?, updated_at = ?
                        WHERE feed_id = ? AND generation = ? AND status = 'PROCESSING'
                        """,
                errorCode,
                Timestamp.from(now),
                feedId,
                generation
        );
    }

    @Override
    public void markGenerationFailed(long feedId, int generation, String errorCode, Instant now) {
        jdbcTemplate.update(
                """
                        UPDATE question_recommendation_jobs
                        SET status = 'FAILED', lease_until = NULL, last_error = ?, updated_at = ?
                        WHERE feed_id = ? AND generation = ? AND status IN ('PENDING', 'PROCESSING')
                        """,
                errorCode,
                Timestamp.from(now),
                feedId,
                generation
        );
    }

    private RowMapper<QuestionRecommendationItem> recommendationItemRowMapper() {
        return (resultSet, rowNumber) -> new QuestionRecommendationItem(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("content")
        );
    }

    private RowMapper<GenerationTask> generationTaskRowMapper() {
        return (resultSet, rowNumber) -> new GenerationTask(
                resultSet.getLong("feed_id"),
                resultSet.getInt("generation"),
                resultSet.getString("source_title"),
                resultSet.getString("source_content"),
                resultSet.getInt("attempt_count")
        );
    }

    private record RecommendationJob(int generation, QuestionRecommendationStatus status) {
    }

    private record CurrentJob(int generation, QuestionRecommendationStatus status) {
    }
}

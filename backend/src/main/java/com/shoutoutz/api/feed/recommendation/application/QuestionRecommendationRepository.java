package com.shoutoutz.api.feed.recommendation.application;

import com.shoutoutz.api.feed.recommendation.application.dto.GeneratedQuestion;
import com.shoutoutz.api.feed.recommendation.application.dto.QuestionRecommendationItem;
import com.shoutoutz.api.feed.recommendation.application.dto.RelatedQuestionRecommendations;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRecommendationRepository {

    int requestGeneration(long feedId, String title, String content, UUID outboxId, Instant now);

    Optional<RelatedQuestionRecommendations> findByFeedId(long feedId);

    List<QuestionRecommendationItem> findRandomItems(int limit);

    List<OutboxMessage> claimOutbox(Instant now, Instant leaseUntil, int limit);

    void markOutboxSent(UUID outboxId, Instant now);

    void retryOutbox(
            OutboxMessage message,
            String errorCode,
            Instant nextAttemptAt,
            int maxAttempts,
            Instant now
    );

    GenerationClaim claimGeneration(long feedId, int generation, Instant now, Instant leaseUntil);

    boolean completeGeneration(
            long feedId,
            int generation,
            List<GeneratedQuestion> questions,
            Instant now
    );

    void retryGeneration(long feedId, int generation, String errorCode, Instant now);

    void markGenerationFailed(long feedId, int generation, String errorCode, Instant now);

    record OutboxMessage(UUID id, long feedId, int generation, int attempts) {
    }

    record GenerationTask(
            long feedId,
            int generation,
            String title,
            String content,
            int attempts
    ) {
    }

    record GenerationClaim(ClaimStatus status, GenerationTask task) {
        public enum ClaimStatus {
            CLAIMED,
            BUSY,
            OBSOLETE
        }
    }
}

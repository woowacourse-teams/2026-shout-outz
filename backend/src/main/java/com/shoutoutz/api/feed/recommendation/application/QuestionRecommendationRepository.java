package com.shoutoutz.api.feed.recommendation.application;

import com.shoutoutz.api.feed.recommendation.application.dto.GeneratedQuestion;
import com.shoutoutz.api.feed.recommendation.application.dto.QuestionRecommendationItem;
import com.shoutoutz.api.feed.recommendation.application.dto.RelatedQuestionRecommendations;
import com.shoutoutz.api.feed.recommendation.domain.QuestionRecommendationJobKind;
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

    Optional<ConsumedRecommendation> consume(long recommendationId, long consumerFeedId, Instant now);

    void requestReplenish(long feedId, int generation, UUID outboxId, Instant now);

    ReplenishClaim claimReplenish(long feedId, int generation, Instant now, Instant leaseUntil);

    List<GenerationItem> findGenerationItems(long feedId, int generation);

    boolean appendItems(long feedId, int generation, List<GeneratedQuestion> questions, Instant now);

    void releaseReplenish(long feedId, int generation, String errorCode, Instant now);

    record OutboxMessage(
            UUID id,
            long feedId,
            int generation,
            int attempts,
            QuestionRecommendationJobKind kind
    ) {
    }

    record ConsumedRecommendation(long sourceFeedId, int generation) {
    }

    record GenerationItem(String title, boolean consumed) {
    }

    record ReplenishTask(
            long feedId,
            int generation,
            String title,
            String content,
            int replenishedCount,
            int attempts
    ) {
    }

    record ReplenishClaim(GenerationClaim.ClaimStatus status, ReplenishTask task) {
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

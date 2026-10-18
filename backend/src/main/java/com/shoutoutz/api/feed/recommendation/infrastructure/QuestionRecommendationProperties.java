package com.shoutoutz.api.feed.recommendation.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "question-recommendation")
public record QuestionRecommendationProperties(
        @DefaultValue("5") int count,
        @DefaultValue("5") int maxGenerationAttempts,
        @DefaultValue("5") int maxOutboxAttempts,
        @DefaultValue("5000") long outboxPollIntervalMillis,
        @DefaultValue("true") boolean fallbackEnabled,
        @DefaultValue("5") int maxReplenishPerGeneration,
        Sqs sqs
) {

    private static final int DEFAULT_MAX_REPLENISH_PER_GENERATION = 5;

    @ConstructorBinding
    public QuestionRecommendationProperties {
        if (count < 1 || count > 10) {
            throw new IllegalArgumentException("question-recommendation.count는 1 이상 10 이하여야 합니다.");
        }
        if (maxGenerationAttempts < 1 || maxOutboxAttempts < 1) {
            throw new IllegalArgumentException("질문 추천 재시도 횟수는 1 이상이어야 합니다.");
        }
        if (maxReplenishPerGeneration < 0) {
            throw new IllegalArgumentException("question-recommendation.max-replenish-per-generation은 0 이상이어야 합니다.");
        }
        sqs = sqs == null ? new Sqs(false, null, null, "ap-northeast-2", 1_000, 1,
                Duration.ofMinutes(3)) : sqs;
        if (sqs.enabled() && (isBlank(sqs.queueUrl()) || isBlank(sqs.deadLetterQueueUrl()))) {
            throw new IllegalArgumentException(
                    "SQS 질문 추천을 활성화하려면 queue-url과 dead-letter-queue-url을 설정해야 합니다."
            );
        }
    }

    public QuestionRecommendationProperties(
            int count,
            int maxGenerationAttempts,
            int maxOutboxAttempts,
            long outboxPollIntervalMillis,
            boolean fallbackEnabled,
            Sqs sqs
    ) {
        this(count, maxGenerationAttempts, maxOutboxAttempts, outboxPollIntervalMillis, fallbackEnabled,
                DEFAULT_MAX_REPLENISH_PER_GENERATION, sqs);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record Sqs(
            @DefaultValue("false") boolean enabled,
            String queueUrl,
            String deadLetterQueueUrl,
            @DefaultValue("ap-northeast-2") String region,
            @DefaultValue("1000") long pollIntervalMillis,
            @DefaultValue("1") int maxMessagesPerPoll,
            @DefaultValue("3m") Duration visibilityTimeout
    ) {
        public Sqs {
            if (maxMessagesPerPoll < 1 || maxMessagesPerPoll > 10) {
                throw new IllegalArgumentException("SQS max-messages-per-poll은 1 이상 10 이하여야 합니다.");
            }
            if (pollIntervalMillis < 100) {
                throw new IllegalArgumentException("SQS poll-interval은 100ms 이상이어야 합니다.");
            }
            if (visibilityTimeout == null || visibilityTimeout.isNegative() || visibilityTimeout.isZero()) {
                throw new IllegalArgumentException("SQS visibility-timeout은 양수여야 합니다.");
            }
        }
    }
}

package com.shoutoutz.api.feed.recommendation.infrastructure;

import com.shoutoutz.api.common.llm.LlmProvider;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "question-recommendation")
public record QuestionRecommendationProperties(
        @DefaultValue("5") int count,
        @DefaultValue("5") int maxGenerationAttempts,
        @DefaultValue("5") int maxOutboxAttempts,
        @DefaultValue("5000") long outboxPollIntervalMillis,
        @DefaultValue("true") boolean fallbackEnabled,
        Sqs sqs,
        @DefaultValue({"BEDROCK_GPT", "BEDROCK_CLAUDE"}) List<LlmProvider> providers
) {
    private static final List<LlmProvider> DEFAULT_PROVIDERS = List.of(LlmProvider.BEDROCK_GPT, LlmProvider.BEDROCK_CLAUDE);

    public QuestionRecommendationProperties {
        if (count < 1 || count > 10) {
            throw new IllegalArgumentException("question-recommendation.count는 1 이상 10 이하여야 합니다.");
        }
        if (maxGenerationAttempts < 1 || maxOutboxAttempts < 1) {
            throw new IllegalArgumentException("질문 추천 재시도 횟수는 1 이상이어야 합니다.");
        }
        sqs = sqs == null ? new Sqs(false, null, null, "ap-northeast-2", 1_000, 1,
                Duration.ofMinutes(3)) : sqs;
        providers = providers == null || providers.isEmpty() ? DEFAULT_PROVIDERS : List.copyOf(providers);
        if (new HashSet<>(providers).size() != providers.size()) {
            throw new IllegalArgumentException("question-recommendation.providers에 같은 제공자를 두 번 넣을 수 없습니다.");
        }
        if (sqs.enabled() && (isBlank(sqs.queueUrl()) || isBlank(sqs.deadLetterQueueUrl()))) {
            throw new IllegalArgumentException(
                    "SQS 질문 추천을 활성화하려면 queue-url과 dead-letter-queue-url을 설정해야 합니다."
            );
        }
    }

    /** 실제로 호출할 제공자 순서. 자동 대체가 꺼지면 첫 제공자만 쓴다. */
    public List<LlmProvider> llmProviders() {
        return fallbackEnabled ? providers : providers.subList(0, 1);
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

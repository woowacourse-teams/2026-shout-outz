package com.shoutoutz.api.feed.recommendation.application;

import com.shoutoutz.api.feed.recommendation.infrastructure.QuestionRecommendationProperties;
import tools.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "question-recommendation.sqs",
        name = "enabled",
        havingValue = "true"
)
public class QuestionRecommendationOutboxDispatcher {

    private static final int CLAIM_LIMIT = 10;
    private static final int OUTBOX_LEASE_SECONDS = 60;

    private final QuestionRecommendationRepository repository;
    private final QuestionRecommendationProperties properties;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Scheduled(
            initialDelayString = "${question-recommendation.outbox-initial-delay-millis:1000}",
            fixedDelayString = "${question-recommendation.outbox-poll-interval-millis:5000}"
    )
    public void dispatchDueMessages() {
        Instant now = clock.instant();
        List<QuestionRecommendationRepository.OutboxMessage> messages = repository.claimOutbox(
                now,
                now.plusSeconds(OUTBOX_LEASE_SECONDS),
                CLAIM_LIMIT
        );
        for (QuestionRecommendationRepository.OutboxMessage message : messages) {
            publish(message);
        }
    }

    private void publish(QuestionRecommendationRepository.OutboxMessage message) {
        try {
            String body = objectMapper.writeValueAsString(
                    new QuestionRecommendationMessage(message.feedId(), message.generation())
            );
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(properties.sqs().queueUrl())
                    .messageBody(body)
                    .build());
            repository.markOutboxSent(message.id(), clock.instant());
        } catch (RuntimeException exception) {
            Instant now = clock.instant();
            long delaySeconds = Math.min(3_600, 5L << Math.min(message.attempts() - 1, 9));
            repository.retryOutbox(
                    message,
                    "OUTBOX_PUBLISH_FAILED",
                    now.plusSeconds(delaySeconds),
                    properties.maxOutboxAttempts(),
                    now
            );
            log.warn(
                    "질문 추천 Outbox 전달에 실패했습니다. feedId={}, generation={}, attempts={}, reason={}",
                    message.feedId(), message.generation(), message.attempts(), exception.getClass().getSimpleName()
            );
        }
    }

    public record QuestionRecommendationMessage(long feedId, int generation) {
    }
}

package com.shoutoutz.api.feed.recommendation.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationOutboxDispatcher.QuestionRecommendationMessage;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationRepository;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationWorker;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "question-recommendation.sqs",
        name = "enabled",
        havingValue = "true"
)
public class QuestionRecommendationSqsPoller {

    private final SqsClient sqsClient;
    private final QuestionRecommendationWorker worker;
    private final QuestionRecommendationRepository repository;
    private final QuestionRecommendationProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Scheduled(
            fixedDelayString = "${question-recommendation.sqs.poll-interval-millis:1000}",
            scheduler = "questionRecommendationTaskScheduler"
    )
    public void pollWorkQueue() {
        List<Message> messages = receive(properties.sqs().queueUrl());
        for (Message message : messages) {
            processWorkMessage(message);
        }
    }

    @Scheduled(
            fixedDelayString = "${question-recommendation.sqs.poll-interval-millis:1000}",
            scheduler = "questionRecommendationTaskScheduler"
    )
    public void pollDeadLetterQueue() {
        List<Message> messages = receive(properties.sqs().deadLetterQueueUrl());
        for (Message message : messages) {
            processDeadLetterMessage(message);
        }
    }

    private List<Message> receive(String queueUrl) {
        return sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(properties.sqs().maxMessagesPerPoll())
                        .waitTimeSeconds(1)
                        .visibilityTimeout(Math.toIntExact(properties.sqs().visibilityTimeout().toSeconds()))
                        .build())
                .messages();
    }

    private void processWorkMessage(Message message) {
        QuestionRecommendationMessage payload;
        try {
            payload = objectMapper.readValue(message.body(), QuestionRecommendationMessage.class);
        } catch (Exception exception) {
            log.warn("질문 추천 SQS 메시지를 해석하지 못했습니다. reason={}", exception.getClass().getSimpleName());
            return;
        }

        try {
            QuestionRecommendationWorker.ProcessingResult result = worker.process(
                    payload.feedId(),
                    payload.generation()
            );
            if (result.acknowledged()) {
                delete(properties.sqs().queueUrl(), message);
            } else {
                sqsClient.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
                        .queueUrl(properties.sqs().queueUrl())
                        .receiptHandle(message.receiptHandle())
                        .visibilityTimeout(Math.toIntExact(result.retryDelaySeconds()))
                        .build());
            }
        } catch (RuntimeException exception) {
            log.warn("질문 추천 SQS 메시지 처리 중 오류가 발생했습니다. reason={}",
                    exception.getClass().getSimpleName());
        }
    }

    private void processDeadLetterMessage(Message message) {
        QuestionRecommendationMessage payload;
        try {
            payload = objectMapper.readValue(
                    message.body(),
                    QuestionRecommendationMessage.class
            );
        } catch (Exception exception) {
            log.warn("질문 추천 DLQ 메시지를 해석하지 못해 제거합니다. reason={}",
                    exception.getClass().getSimpleName());
            delete(properties.sqs().deadLetterQueueUrl(), message);
            return;
        }
        try {
            repository.markGenerationFailed(
                    payload.feedId(),
                    payload.generation(),
                    "MAX_RETRIES_EXCEEDED",
                    clock.instant()
            );
            delete(properties.sqs().deadLetterQueueUrl(), message);
        } catch (Exception exception) {
            log.warn("질문 추천 DLQ 메시지의 상태를 저장하지 못했습니다. 재처리하도록 남깁니다. reason={}",
                    exception.getClass().getSimpleName());
        }
    }

    private void delete(String queueUrl, Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build());
    }
}

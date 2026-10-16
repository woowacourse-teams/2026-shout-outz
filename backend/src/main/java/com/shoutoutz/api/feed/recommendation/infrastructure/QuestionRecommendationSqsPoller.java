package com.shoutoutz.api.feed.recommendation.infrastructure;

import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationOutboxDispatcher.QuestionRecommendationMessage;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationRepository;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationWorker;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@ConditionalOnProperty(
        prefix = "question-recommendation.sqs",
        name = "enabled",
        havingValue = "true"
)
public class QuestionRecommendationSqsPoller {

    private static final long MAX_VISIBILITY_SECONDS = 43_200;
    private static final long VISIBILITY_SAFETY_MARGIN_SECONDS = 5;

    private final SqsClient sqsClient;
    private final QuestionRecommendationWorker worker;
    private final QuestionRecommendationRepository repository;
    private final QuestionRecommendationProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final LongSupplier ticker;

    @Autowired
    public QuestionRecommendationSqsPoller(
            SqsClient sqsClient,
            QuestionRecommendationWorker worker,
            QuestionRecommendationRepository repository,
            QuestionRecommendationProperties properties,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this(sqsClient, worker, repository, properties, objectMapper, clock, System::nanoTime);
    }

    QuestionRecommendationSqsPoller(
            SqsClient sqsClient,
            QuestionRecommendationWorker worker,
            QuestionRecommendationRepository repository,
            QuestionRecommendationProperties properties,
            ObjectMapper objectMapper,
            Clock clock,
            LongSupplier ticker
    ) {
        this.sqsClient = sqsClient;
        this.worker = worker;
        this.repository = repository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.ticker = ticker;
    }

    @Scheduled(
            fixedDelayString = "${question-recommendation.sqs.poll-interval-millis:1000}",
            scheduler = "questionRecommendationTaskScheduler"
    )
    public void pollWorkQueue() {
        ReceivedMessages received = receive(properties.sqs().queueUrl());
        for (Message message : received.messages()) {
            processWorkMessage(message, received.startedAtNanos());
        }
    }

    @Scheduled(
            fixedDelayString = "${question-recommendation.sqs.poll-interval-millis:1000}",
            scheduler = "questionRecommendationTaskScheduler"
    )
    public void pollDeadLetterQueue() {
        ReceivedMessages received = receive(properties.sqs().deadLetterQueueUrl());
        for (Message message : received.messages()) {
            processDeadLetterMessage(message);
        }
    }

    private ReceivedMessages receive(String queueUrl) {
        long startedAtNanos = ticker.getAsLong();
        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(properties.sqs().maxMessagesPerPoll())
                        .waitTimeSeconds(1)
                        .visibilityTimeout(Math.toIntExact(properties.sqs().visibilityTimeout().toSeconds()))
                        .build())
                .messages();
        return new ReceivedMessages(messages, startedAtNanos);
    }

    private void processWorkMessage(Message message, long receivedAtNanos) {
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
                // SQS의 12시간 한도는 수신부터 계산하므로 처리 시간과 네트워크 여유를 뺀다.
                long elapsedSeconds = TimeUnit.NANOSECONDS.toSeconds(
                        Math.max(0, ticker.getAsLong() - receivedAtNanos)
                );
                long remainingSeconds = MAX_VISIBILITY_SECONDS - elapsedSeconds - VISIBILITY_SAFETY_MARGIN_SECONDS;
                if (remainingSeconds <= 0) {
                    log.warn("질문 추천 SQS 메시지의 가시성 연장 가능 시간이 지났습니다. feedId={}, generation={}",
                            payload.feedId(), payload.generation());
                    return;
                }
                long delaySeconds = Math.min(result.retryDelaySeconds(), remainingSeconds);
                sqsClient.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
                        .queueUrl(properties.sqs().queueUrl())
                        .receiptHandle(message.receiptHandle())
                        .visibilityTimeout(Math.toIntExact(delaySeconds))
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

    private record ReceivedMessages(List<Message> messages, long startedAtNanos) {
    }
}

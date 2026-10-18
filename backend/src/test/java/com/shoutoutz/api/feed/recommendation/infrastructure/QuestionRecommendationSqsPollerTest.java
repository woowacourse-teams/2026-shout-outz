package com.shoutoutz.api.feed.recommendation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationRepository;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationWorker;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationWorker.ProcessingResult;
import com.shoutoutz.api.feed.recommendation.domain.QuestionRecommendationJobKind;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class QuestionRecommendationSqsPollerTest {

    private static final Instant NOW = Instant.parse("2026-10-15T00:00:00Z");

    @Mock
    private SqsClient sqsClient;
    @Mock
    private QuestionRecommendationWorker worker;
    @Mock
    private QuestionRecommendationRepository repository;

    private final AtomicLong nanos = new AtomicLong();
    private QuestionRecommendationSqsPoller poller;

    @BeforeEach
    void setUp() {
        QuestionRecommendationProperties properties = new QuestionRecommendationProperties(
                5, 3, 5, 5_000, true,
                new QuestionRecommendationProperties.Sqs(
                        true, "work-queue", "dead-letter-queue", "ap-northeast-2", 1_000, 2,
                        Duration.ofMinutes(3)
                )
        );
        poller = new QuestionRecommendationSqsPoller(
                sqsClient, worker, repository, properties, new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC), nanos::get
        );
    }

    @Test
    void 수신_요청과_제공자_호출_경과시간을_빼고_가시성_시간을_제한한다() {
        receive(message(10), 2);
        when(worker.process(10, 1, QuestionRecommendationJobKind.GENERATE)).thenAnswer(invocation -> {
            advanceSeconds(30);
            return ProcessingResult.retry(43_200);
        });

        poller.pollWorkQueue();

        ArgumentCaptor<ChangeMessageVisibilityRequest> request = ArgumentCaptor.forClass(ChangeMessageVisibilityRequest.class);
        verify(sqsClient).changeMessageVisibility(request.capture());
        assertThat(request.getValue().visibilityTimeout()).isEqualTo(43_163);
        assertThat(request.getValue().queueUrl()).isEqualTo("work-queue");
        assertThat(request.getValue().receiptHandle()).isEqualTo("receipt-10");
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void 배치의_다음_메시지는_앞선_처리시간까지_포함해_남은_시간을_계산한다() {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenAnswer(invocation -> {
            advanceSeconds(2);
            return ReceiveMessageResponse.builder().messages(message(10), message(11)).build();
        });
        when(worker.process(10, 1, QuestionRecommendationJobKind.GENERATE)).thenAnswer(invocation -> {
            advanceSeconds(20);
            return ProcessingResult.retry(43_200);
        });
        when(worker.process(11, 1, QuestionRecommendationJobKind.GENERATE)).thenAnswer(invocation -> {
            advanceSeconds(30);
            return ProcessingResult.retry(43_200);
        });

        poller.pollWorkQueue();

        ArgumentCaptor<ChangeMessageVisibilityRequest> requests = ArgumentCaptor.forClass(ChangeMessageVisibilityRequest.class);
        verify(sqsClient, times(2)).changeMessageVisibility(requests.capture());
        assertThat(requests.getAllValues()).extracting(ChangeMessageVisibilityRequest::visibilityTimeout)
                .containsExactly(43_173, 43_143);
    }

    @Test
    void 남은_시간보다_짧은_일반_재시도_대기시간은_그대로_사용한다() {
        receive(message(10), 2);
        when(worker.process(10, 1, QuestionRecommendationJobKind.GENERATE)).thenReturn(ProcessingResult.retry(120));

        poller.pollWorkQueue();

        ArgumentCaptor<ChangeMessageVisibilityRequest> request = ArgumentCaptor.forClass(ChangeMessageVisibilityRequest.class);
        verify(sqsClient).changeMessageVisibility(request.capture());
        assertThat(request.getValue().visibilityTimeout()).isEqualTo(120);
    }

    @Test
    void 가시성_연장_가능_시간이_없으면_잘못된_연장_요청을_보내지_않는다() {
        receive(message(10), 43_200);
        when(worker.process(10, 1, QuestionRecommendationJobKind.GENERATE)).thenReturn(ProcessingResult.retry(43_200));

        poller.pollWorkQueue();

        verify(sqsClient, never()).changeMessageVisibility(any(ChangeMessageVisibilityRequest.class));
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void 완료된_작업은_가시성을_연장하지_않고_삭제한다() {
        receive(message(10), 0);
        when(worker.process(10, 1, QuestionRecommendationJobKind.GENERATE)).thenReturn(ProcessingResult.done());

        poller.pollWorkQueue();

        verify(sqsClient).deleteMessage(DeleteMessageRequest.builder()
                .queueUrl("work-queue").receiptHandle("receipt-10").build());
        verify(sqsClient, never()).changeMessageVisibility(any(ChangeMessageVisibilityRequest.class));
    }

    @Test
    void DLQ_메시지는_실패_상태를_저장한_뒤_삭제한다() {
        receive(message(10), 0);

        poller.pollDeadLetterQueue();

        InOrder order = inOrder(repository, sqsClient);
        order.verify(repository).markGenerationFailed(10, 1, "MAX_RETRIES_EXCEEDED", NOW);
        order.verify(sqsClient).deleteMessage(DeleteMessageRequest.builder()
                .queueUrl("dead-letter-queue").receiptHandle("receipt-10").build());
        verifyNoInteractions(worker);
    }

    @Test
    void DLQ_상태_저장에_실패하면_메시지를_삭제하지_않는다() {
        receive(message(10), 0);
        doThrow(new IllegalStateException("database unavailable")).when(repository)
                .markGenerationFailed(10, 1, "MAX_RETRIES_EXCEEDED", NOW);

        poller.pollDeadLetterQueue();

        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void 대체_생성_메시지는_대체_생성_작업으로_처리한다() {
        receive(replenishMessage(10), 0);
        when(worker.process(10, 1, QuestionRecommendationJobKind.REPLENISH)).thenReturn(ProcessingResult.done());

        poller.pollWorkQueue();

        verify(worker).process(10, 1, QuestionRecommendationJobKind.REPLENISH);
        verify(sqsClient).deleteMessage(DeleteMessageRequest.builder()
                .queueUrl("work-queue").receiptHandle("receipt-10").build());
    }

    @Test
    void 대체_생성_DLQ_메시지는_작업을_실패로_바꾸지_않고_선점만_해제한다() {
        receive(replenishMessage(10), 0);

        poller.pollDeadLetterQueue();

        verify(repository).releaseReplenish(10, 1, "MAX_RETRIES_EXCEEDED", NOW);
        verify(repository, never()).markGenerationFailed(10, 1, "MAX_RETRIES_EXCEEDED", NOW);
        verify(sqsClient).deleteMessage(DeleteMessageRequest.builder()
                .queueUrl("dead-letter-queue").receiptHandle("receipt-10").build());
    }

    private Message replenishMessage(long feedId) {
        return Message.builder().body("{\"feedId\":" + feedId + ",\"generation\":1,\"kind\":\"REPLENISH\"}")
                .receiptHandle("receipt-" + feedId).build();
    }

    private void receive(Message message, long elapsedSeconds) {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenAnswer(invocation -> {
            advanceSeconds(elapsedSeconds);
            return ReceiveMessageResponse.builder().messages(message).build();
        });
    }

    private Message message(long feedId) {
        return Message.builder().body("{\"feedId\":" + feedId + ",\"generation\":1}")
                .receiptHandle("receipt-" + feedId).build();
    }

    private void advanceSeconds(long seconds) {
        nanos.addAndGet(TimeUnit.SECONDS.toNanos(seconds));
    }
}

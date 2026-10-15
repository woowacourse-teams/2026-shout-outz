package com.shoutoutz.api.feed.recommendation.application;

import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationRepository.GenerationClaim;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationRepository.GenerationTask;
import com.shoutoutz.api.feed.recommendation.application.dto.GeneratedQuestion;
import com.shoutoutz.api.feed.recommendation.infrastructure.QuestionRecommendationProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "question-recommendation.sqs",
        name = "enabled",
        havingValue = "true"
)
public class QuestionRecommendationWorker {

    public static final int GENERATION_LEASE_SECONDS = 180;

    private final QuestionRecommendationRepository repository;
    private final QuestionRecommendationGenerator generator;
    private final QuestionRecommendationProperties properties;
    private final Clock clock;
    private final MeterRegistry meterRegistry;
    private final QuestionRecommendationRetryPolicy retryPolicy;

    public ProcessingResult process(long feedId, int generation) {
        Instant now = clock.instant();
        GenerationClaim claim = repository.claimGeneration(
                feedId,
                generation,
                now,
                now.plusSeconds(GENERATION_LEASE_SECONDS)
        );
        if (claim.status() == GenerationClaim.ClaimStatus.OBSOLETE) {
            return ProcessingResult.done();
        }
        if (claim.status() == GenerationClaim.ClaimStatus.BUSY) {
            return ProcessingResult.retry(GENERATION_LEASE_SECONDS);
        }

        GenerationTask task = claim.task();
        if (task.attempts() > properties.maxGenerationAttempts()) {
            log.warn(
                    "질문 추천 최대 시도 횟수를 초과하여 DLQ 이동을 기다립니다. feedId={}, generation={}, attempts={}",
                    task.feedId(), task.generation(), task.attempts()
            );
            return ProcessingResult.retry(GENERATION_LEASE_SECONDS);
        }
        Timer.Sample sample = Timer.start(meterRegistry);
        String previousTraceId = MDC.get("traceId");
        MDC.put("traceId", "recommendation-" + task.feedId() + "-" + task.generation() + "-" + task.attempts());
        String outcome = "success";
        try {
            List<GeneratedQuestion> generated = generator.generate(
                    task.title(),
                    task.content(),
                    properties.count()
            );
            boolean stored = repository.completeGeneration(
                    task.feedId(),
                    task.generation(),
                    generated,
                    clock.instant()
            );
            if (!stored) {
                outcome = "obsolete";
            }
            return ProcessingResult.done();
        } catch (RuntimeException exception) {
            outcome = "failure";
            boolean retryable = !(exception instanceof QuestionRecommendationGenerationException failure)
                    || failure.retryable();
            long retryAfterSeconds = exception instanceof QuestionRecommendationGenerationException failure
                    ? failure.retryAfterSeconds() : 0;
            long delaySeconds = task.attempts() < properties.maxGenerationAttempts()
                    ? retryPolicy.delaySeconds(task.attempts(), retryAfterSeconds) : GENERATION_LEASE_SECONDS;
            log.warn(
                    "질문 추천 생성에 실패했습니다. feedId={}, generation={}, attempts={}, reason={}, "
                            + "retryable={}, retryDelaySeconds={}",
                    task.feedId(), task.generation(), task.attempts(), exception.getClass().getSimpleName(),
                    retryable, retryable ? delaySeconds : 0
            );
            if (!retryable) {
                repository.markGenerationFailed(
                        task.feedId(), task.generation(), "GENERATION_FAILED", clock.instant()
                );
                return ProcessingResult.done();
            }
            if (task.attempts() < properties.maxGenerationAttempts()) {
                repository.retryGeneration(
                        task.feedId(),
                        task.generation(),
                        "GENERATION_FAILED",
                        clock.instant()
                );
                return ProcessingResult.retry(delaySeconds);
            }
            // SQS redrive policy moves this message to the DLQ after maxReceiveCount.
            // Keep the lease until then so another worker cannot start an extra attempt.
            return ProcessingResult.retry(GENERATION_LEASE_SECONDS);
        } finally {
            try {
                sample.stop(Timer.builder("question.recommendation.generation")
                        .description("질문 추천 생성 시도 소요 시간")
                        .tag("outcome", outcome)
                        .register(meterRegistry));
                meterRegistry.counter("question.recommendation.generation.count", "outcome", outcome)
                        .increment();
            } finally {
                if (previousTraceId == null) {
                    MDC.remove("traceId");
                } else {
                    MDC.put("traceId", previousTraceId);
                }
            }
        }
    }

    public record ProcessingResult(boolean acknowledged, long retryDelaySeconds) {

        public static ProcessingResult done() {
            return new ProcessingResult(true, 0);
        }

        public static ProcessingResult retry(long delaySeconds) {
            return new ProcessingResult(false, delaySeconds);
        }
    }
}

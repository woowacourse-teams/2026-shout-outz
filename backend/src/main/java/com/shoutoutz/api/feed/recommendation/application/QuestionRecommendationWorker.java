package com.shoutoutz.api.feed.recommendation.application;

import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationRepository.GenerationClaim;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationRepository.GenerationTask;
import com.shoutoutz.api.feed.recommendation.application.dto.GeneratedQuestion;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final int GENERATION_LEASE_SECONDS = 180;

    private final QuestionRecommendationRepository repository;
    private final QuestionRecommendationGenerator generator;
    private final QuestionRecommendationProperties properties;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

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
        Timer.Sample sample = Timer.start(meterRegistry);
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
            log.warn(
                    "질문 추천 생성에 실패했습니다. feedId={}, generation={}, attempts={}, reason={}",
                    task.feedId(), task.generation(), task.attempts(), exception.getClass().getSimpleName()
            );
            if (task.attempts() < properties.maxGenerationAttempts()) {
                repository.retryGeneration(
                        task.feedId(),
                        task.generation(),
                        "GENERATION_FAILED",
                        clock.instant()
                );
                long delaySeconds = Math.min(300, 5L << Math.min(task.attempts() - 1, 5));
                return ProcessingResult.retry(delaySeconds);
            }
            // SQS redrive policy moves this message to the DLQ after maxReceiveCount.
            // Keep the lease until then so another worker cannot start an extra attempt.
            long delaySeconds = GENERATION_LEASE_SECONDS;
            return ProcessingResult.retry(delaySeconds);
        } finally {
            sample.stop(Timer.builder("question.recommendation.generation")
                    .description("질문 추천 생성 시도 소요 시간")
                    .tag("outcome", outcome)
                    .register(meterRegistry));
            meterRegistry.counter("question.recommendation.generation.count", "outcome", outcome)
                    .increment();
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

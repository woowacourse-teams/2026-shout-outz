package com.shoutoutz.api.feed.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationRepository.GenerationClaim;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationRepository.GenerationTask;
import com.shoutoutz.api.feed.recommendation.application.dto.GeneratedQuestion;
import com.shoutoutz.api.feed.recommendation.infrastructure.QuestionRecommendationProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class QuestionRecommendationWorkerTest {

    private static final long FEED_ID = 10L;
    private static final int GENERATION = 2;
    private static final Instant NOW = Instant.parse("2026-10-15T00:00:00Z");

    @Mock
    private QuestionRecommendationRepository repository;

    @Mock
    private QuestionRecommendationGenerator generator;

    private QuestionRecommendationWorker worker;

    @BeforeEach
    void setUp() {
        worker = new QuestionRecommendationWorker(
                repository,
                generator,
                new QuestionRecommendationProperties(5, 3, 5, 5_000, true, null),
                Clock.fixed(NOW, ZoneOffset.UTC),
                new SimpleMeterRegistry(),
                new QuestionRecommendationRetryPolicy(bound -> 0)
        );
    }

    @Test
    void 작업을_선점하면_추천_5개를_생성하고_저장한다() {
        GenerationTask task = new GenerationTask(FEED_ID, GENERATION, "질문 제목", "질문 본문", 1);
        List<GeneratedQuestion> generated = List.of(
                new GeneratedQuestion("추천 제목", "추천 본문")
        );
        when(repository.claimGeneration(FEED_ID, GENERATION, NOW, NOW.plusSeconds(180)))
                .thenReturn(claimed(task));
        when(generator.generate("질문 제목", "질문 본문", 5)).thenReturn(generated);
        when(repository.completeGeneration(FEED_ID, GENERATION, generated, NOW)).thenReturn(true);

        QuestionRecommendationWorker.ProcessingResult result = worker.process(FEED_ID, GENERATION);

        assertThat(result.acknowledged()).isTrue();
        verify(generator).generate("질문 제목", "질문 본문", 5);
        verify(repository).completeGeneration(FEED_ID, GENERATION, generated, NOW);
    }

    @Test
    void 생성에_실패하면_재시도_상태를_기록하고_지연_후_재처리한다() {
        GenerationTask task = new GenerationTask(FEED_ID, GENERATION, "질문 제목", "질문 본문", 1);
        when(repository.claimGeneration(FEED_ID, GENERATION, NOW, NOW.plusSeconds(180)))
                .thenReturn(claimed(task));
        when(generator.generate("질문 제목", "질문 본문", 5))
                .thenThrow(new IllegalStateException("generator unavailable"));

        QuestionRecommendationWorker.ProcessingResult result = worker.process(FEED_ID, GENERATION);

        assertThat(result.acknowledged()).isFalse();
        assertThat(result.retryDelaySeconds()).isEqualTo(5L);
        verify(repository).retryGeneration(FEED_ID, GENERATION, "GENERATION_FAILED", NOW);
    }

    @Test
    void 최대_시도_횟수에_도달하면_SQS_DLQ_이동까지_선점을_유지한다() {
        GenerationTask task = new GenerationTask(FEED_ID, GENERATION, "질문 제목", "질문 본문", 3);
        when(repository.claimGeneration(FEED_ID, GENERATION, NOW, NOW.plusSeconds(180)))
                .thenReturn(claimed(task));
        when(generator.generate("질문 제목", "질문 본문", 5))
                .thenThrow(new IllegalStateException("generator unavailable"));

        QuestionRecommendationWorker.ProcessingResult result = worker.process(FEED_ID, GENERATION);

        assertThat(result.acknowledged()).isFalse();
        assertThat(result.retryDelaySeconds()).isEqualTo(180L);
        verify(repository, never()).retryGeneration(FEED_ID, GENERATION, "GENERATION_FAILED", NOW);
        verify(repository, never()).markGenerationFailed(FEED_ID, GENERATION, "GENERATION_FAILED", NOW);
    }

    @Test
    void 두_제공자가_일시적으로_실패하면_제공자의_재시도_대기시간을_존중한다() {
        GenerationTask task = new GenerationTask(FEED_ID, GENERATION, "질문 제목", "질문 본문", 1);
        when(repository.claimGeneration(FEED_ID, GENERATION, NOW, NOW.plusSeconds(180)))
                .thenReturn(claimed(task));
        when(generator.generate("질문 제목", "질문 본문", 5))
                .thenThrow(new QuestionRecommendationGenerationException(true, 120));

        QuestionRecommendationWorker.ProcessingResult result = worker.process(FEED_ID, GENERATION);

        assertThat(result.acknowledged()).isFalse();
        assertThat(result.retryDelaySeconds()).isEqualTo(120);
        verify(repository).retryGeneration(FEED_ID, GENERATION, "GENERATION_FAILED", NOW);
    }

    @Test
    void 추천에_부적합한_질문이면_건너뜀_상태로_저장하고_재시도하지_않는다() {
        GenerationTask task = new GenerationTask(FEED_ID, GENERATION, "질문 제목", "질문 본문", 1);
        when(repository.claimGeneration(FEED_ID, GENERATION, NOW, NOW.plusSeconds(180)))
                .thenReturn(claimed(task));
        when(generator.generate("질문 제목", "질문 본문", 5))
                .thenThrow(new QuestionRecommendationSkippedException("TOO_VAGUE"));

        QuestionRecommendationWorker.ProcessingResult result = worker.process(FEED_ID, GENERATION);

        assertThat(result.acknowledged()).isTrue();
        verify(repository).markGenerationSkipped(FEED_ID, GENERATION, "TOO_VAGUE", NOW);
        verify(repository, never()).markGenerationFailed(FEED_ID, GENERATION, "GENERATION_FAILED", NOW);
        verify(repository, never()).retryGeneration(FEED_ID, GENERATION, "GENERATION_FAILED", NOW);
    }

    @Test
    void 두_제공자가_재시도_불가능한_오류로_실패하면_실패_저장후_메시지를_확인한다() {
        GenerationTask task = new GenerationTask(FEED_ID, GENERATION, "질문 제목", "질문 본문", 1);
        when(repository.claimGeneration(FEED_ID, GENERATION, NOW, NOW.plusSeconds(180)))
                .thenReturn(claimed(task));
        when(generator.generate("질문 제목", "질문 본문", 5))
                .thenThrow(new QuestionRecommendationGenerationException(false, 0));

        QuestionRecommendationWorker.ProcessingResult result = worker.process(FEED_ID, GENERATION);

        assertThat(result.acknowledged()).isTrue();
        assertThat(result.retryDelaySeconds()).isZero();
        verify(repository).markGenerationFailed(FEED_ID, GENERATION, "GENERATION_FAILED", NOW);
        verify(repository, never()).retryGeneration(FEED_ID, GENERATION, "GENERATION_FAILED", NOW);
    }

    @Test
    void 최대_시도_이후_재수신한_메시지는_제공자를_다시_호출하지_않는다() {
        GenerationTask task = new GenerationTask(FEED_ID, GENERATION, "질문 제목", "질문 본문", 4);
        when(repository.claimGeneration(FEED_ID, GENERATION, NOW, NOW.plusSeconds(180)))
                .thenReturn(claimed(task));

        QuestionRecommendationWorker.ProcessingResult result = worker.process(FEED_ID, GENERATION);

        assertThat(result.acknowledged()).isFalse();
        assertThat(result.retryDelaySeconds()).isEqualTo(180);
        verifyNoInteractions(generator);
        verify(repository, never()).retryGeneration(FEED_ID, GENERATION, "GENERATION_FAILED", NOW);
    }

    @Test
    void 생성_도중_질문이_수정되어_저장되지_않아도_메시지를_확인한다() {
        GenerationTask task = new GenerationTask(FEED_ID, GENERATION, "질문 제목", "질문 본문", 1);
        List<GeneratedQuestion> generated = List.of(new GeneratedQuestion("추천 제목", "추천 본문"));
        when(repository.claimGeneration(FEED_ID, GENERATION, NOW, NOW.plusSeconds(180)))
                .thenReturn(claimed(task));
        when(generator.generate("질문 제목", "질문 본문", 5)).thenReturn(generated);
        when(repository.completeGeneration(FEED_ID, GENERATION, generated, NOW)).thenReturn(false);

        QuestionRecommendationWorker.ProcessingResult result = worker.process(FEED_ID, GENERATION);

        assertThat(result.acknowledged()).isTrue();
        verify(repository, never()).retryGeneration(FEED_ID, GENERATION, "GENERATION_FAILED", NOW);
    }

    @Test
    void 생성_로그를_작업별_traceId로_연결하고_기존_traceId를_복원한다() {
        GenerationTask task = new GenerationTask(FEED_ID, GENERATION, "질문 제목", "질문 본문", 1);
        when(repository.claimGeneration(FEED_ID, GENERATION, NOW, NOW.plusSeconds(180)))
                .thenReturn(claimed(task));
        when(generator.generate("질문 제목", "질문 본문", 5)).thenAnswer(invocation -> {
            assertThat(MDC.get("traceId")).isEqualTo("recommendation-10-2-1");
            throw new QuestionRecommendationGenerationException(false, 0);
        });
        String previousTraceId = MDC.get("traceId");
        MDC.put("traceId", "previous-trace");
        try {
            worker.process(FEED_ID, GENERATION);

            assertThat(MDC.get("traceId")).isEqualTo("previous-trace");
        } finally {
            if (previousTraceId == null) {
                MDC.remove("traceId");
            } else {
                MDC.put("traceId", previousTraceId);
            }
        }
    }

    @Test
    void 기존_traceId가_없으면_생성후_MDC에서_제거한다() {
        GenerationTask task = new GenerationTask(FEED_ID, GENERATION, "질문 제목", "질문 본문", 1);
        when(repository.claimGeneration(FEED_ID, GENERATION, NOW, NOW.plusSeconds(180)))
                .thenReturn(claimed(task));
        when(generator.generate("질문 제목", "질문 본문", 5)).thenAnswer(invocation -> {
            assertThat(MDC.get("traceId")).isEqualTo("recommendation-10-2-1");
            throw new QuestionRecommendationGenerationException(true, 0);
        });

        worker.process(FEED_ID, GENERATION);

        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void 이미_선점되었거나_오래된_작업은_AI를_호출하지_않는다() {
        when(repository.claimGeneration(FEED_ID, GENERATION, NOW, NOW.plusSeconds(180)))
                .thenReturn(new GenerationClaim(GenerationClaim.ClaimStatus.BUSY, null))
                .thenReturn(new GenerationClaim(GenerationClaim.ClaimStatus.OBSOLETE, null));

        QuestionRecommendationWorker.ProcessingResult busy = worker.process(FEED_ID, GENERATION);
        QuestionRecommendationWorker.ProcessingResult obsolete = worker.process(FEED_ID, GENERATION);

        assertThat(busy.acknowledged()).isFalse();
        assertThat(busy.retryDelaySeconds()).isEqualTo(180L);
        assertThat(obsolete.acknowledged()).isTrue();
        verifyNoInteractions(generator);
    }

    private GenerationClaim claimed(GenerationTask task) {
        return new GenerationClaim(GenerationClaim.ClaimStatus.CLAIMED, task);
    }
}

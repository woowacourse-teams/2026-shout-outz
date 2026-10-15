package com.shoutoutz.api.feed.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
                new QuestionRecommendationProperties(5, 3, 5, 5_000, null, null),
                Clock.fixed(NOW, ZoneOffset.UTC),
                new SimpleMeterRegistry()
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
    void 이미_선점되었거나_오래된_작업은_AI를_호출하지_않는다() {
        when(repository.claimGeneration(FEED_ID, GENERATION, NOW, NOW.plusSeconds(180)))
                .thenReturn(new GenerationClaim(GenerationClaim.ClaimStatus.BUSY, null))
                .thenReturn(new GenerationClaim(GenerationClaim.ClaimStatus.OBSOLETE, null));

        QuestionRecommendationWorker.ProcessingResult busy = worker.process(FEED_ID, GENERATION);
        QuestionRecommendationWorker.ProcessingResult obsolete = worker.process(FEED_ID, GENERATION);

        assertThat(busy.acknowledged()).isFalse();
        assertThat(busy.retryDelaySeconds()).isEqualTo(180L);
        assertThat(obsolete.acknowledged()).isTrue();
        verify(generator, never()).generate("질문 제목", "질문 본문", 5);
    }

    private GenerationClaim claimed(GenerationTask task) {
        return new GenerationClaim(GenerationClaim.ClaimStatus.CLAIMED, task);
    }
}

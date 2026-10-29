package com.shoutoutz.api.feed.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.feed.application.dto.FeedItem;
import com.shoutoutz.api.feed.recommendation.application.dto.QuestionRecommendationItem;
import com.shoutoutz.api.feed.recommendation.domain.QuestionRecommendationStatus;
import com.shoutoutz.api.feed.recommendation.infrastructure.QuestionRecommendationProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionRecommendationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-15T00:00:00Z");

    @Mock
    private QuestionRecommendationRepository repository;

    private QuestionRecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new QuestionRecommendationService(
                repository,
                new QuestionRecommendationProperties(5, 5, 5, 5_000, true, null, null),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void 질문_다섯개마다_한개씩_랜덤_추천_슬롯을_만든다() {
        List<QuestionRecommendationItem> recommendations = List.of(
                recommendation(101L),
                recommendation(102L),
                recommendation(103L),
                recommendation(104L)
        );
        when(repository.findRandomItems(4)).thenReturn(recommendations);

        var slots = recommendationService.findListSlots(questions(20));

        assertThat(slots).extracting("afterFeedId").containsExactly(5L, 10L, 15L, 20L);
        assertThat(slots).extracting("recommendation").containsExactlyElementsOf(recommendations);
        verify(repository).findRandomItems(4);
    }

    @Test
    void 질문이_다섯개_미만이면_추천_저장소를_조회하지_않는다() {
        assertThat(recommendationService.findListSlots(questions(4))).isEmpty();
        verify(repository, never()).findRandomItems(any(Integer.class));
    }

    @Test
    void 추천_후보가_부족하면_있는_개수만큼_슬롯을_반환한다() {
        when(repository.findRandomItems(2)).thenReturn(List.of(recommendation(101L)));

        var slots = recommendationService.findListSlots(questions(10));

        assertThat(slots).hasSize(1);
        assertThat(slots.getFirst().afterFeedId()).isEqualTo(5L);
    }

    @Test
    void SQS가_활성화되면_추천_생성을_등록하고_Pending을_반환한다() {
        QuestionRecommendationProperties enabledProperties = new QuestionRecommendationProperties(
                5,
                5,
                5,
                5_000,
                true,
                new QuestionRecommendationProperties.Sqs(
                        true, "queue-url", "dead-letter-queue-url", "ap-northeast-2", 1_000, 1,
                        Duration.ofMinutes(3)
                ),
                null
        );
        recommendationService = new QuestionRecommendationService(
                repository,
                enabledProperties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        when(repository.requestGeneration(eq(10L), eq("질문 제목"), eq("질문 본문"), any(UUID.class), eq(NOW)))
                .thenReturn(1);

        QuestionRecommendationStatus status = recommendationService.requestGeneration(
                10L, "질문 제목", "질문 본문"
        );

        assertThat(status).isEqualTo(QuestionRecommendationStatus.PENDING);
        verify(repository).requestGeneration(
                eq(10L), eq("질문 제목"), eq("질문 본문"), any(UUID.class), eq(NOW)
        );
    }

    @Test
    void 추천_질문을_사용하면_사용_처리하고_원글의_대체_생성을_등록한다() {
        recommendationService = new QuestionRecommendationService(
                repository,
                sqsEnabledProperties(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        when(repository.consume(101L, 20L, NOW))
                .thenReturn(Optional.of(new QuestionRecommendationRepository.ConsumedRecommendation(10L, 3)));

        recommendationService.markUsed(101L, 20L);

        verify(repository).requestReplenish(eq(10L), eq(3), any(UUID.class), eq(NOW));
    }

    @Test
    void 사용할_수_없는_추천이면_예외없이_무시한다() {
        recommendationService = new QuestionRecommendationService(
                repository,
                sqsEnabledProperties(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        when(repository.consume(101L, 20L, NOW)).thenReturn(Optional.empty());

        recommendationService.markUsed(101L, 20L);

        verify(repository, never()).requestReplenish(any(Long.class), any(Integer.class), any(), any());
    }

    @Test
    void SQS가_비활성화되면_사용_처리만_하고_대체_생성은_등록하지_않는다() {
        when(repository.consume(101L, 20L, NOW))
                .thenReturn(Optional.of(new QuestionRecommendationRepository.ConsumedRecommendation(10L, 3)));

        recommendationService.markUsed(101L, 20L);

        verify(repository).consume(101L, 20L, NOW);
        verify(repository, never()).requestReplenish(any(Long.class), any(Integer.class), any(), any());
    }

    private QuestionRecommendationProperties sqsEnabledProperties() {
        return new QuestionRecommendationProperties(
                5,
                5,
                5,
                5_000,
                true,
                new QuestionRecommendationProperties.Sqs(
                        true, "queue-url", "dead-letter-queue-url", "ap-northeast-2", 1_000, 1,
                        Duration.ofMinutes(3)
                ),
                null
        );
    }

    private List<FeedItem> questions(int count) {
        return java.util.stream.LongStream.rangeClosed(1, count)
                .mapToObj(id -> new FeedItem(
                        id,
                        "질문 제목 " + id,
                        "질문 본문 " + id,
                        new FeedItem.Author("@author", "작성자", null, null, null, null),
                        List.of(),
                        List.of(),
                        0L,
                        0L,
                        0,
                        NOW.plusSeconds(id),
                        NOW.plusSeconds(id)
                ))
                .toList();
    }

    private QuestionRecommendationItem recommendation(long id) {
        return new QuestionRecommendationItem(id, "추천 제목 " + id, "추천 본문 " + id);
    }
}

package com.shoutoutz.api.feed.recommendation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.feed.domain.Feed;
import com.shoutoutz.api.feed.domain.FeedRepository;
import com.shoutoutz.api.feed.domain.FeedType;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationRepository;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationRepository.GenerationClaim;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationRepository.GenerationItem;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationRepository.ReplenishClaim;
import com.shoutoutz.api.feed.recommendation.application.dto.GeneratedQuestion;
import com.shoutoutz.api.feed.recommendation.application.dto.QuestionRecommendationItem;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "aws.s3.bucket=test-bucket",
        "aws.s3.region=ap-northeast-2",
        "aws.s3.presigned-url-expiration-seconds=300",
        "spring.flyway.ignore-migration-patterns=*:missing"
})
@Transactional
class QuestionRecommendationRepositoryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-18T00:00:00Z");

    @Autowired
    private QuestionRecommendationRepository repository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 추천_질문은_한_번만_사용할_수_있고_사용된_추천은_상세와_목록에서_제외된다() {
        long authorId = insertUser();
        long sourceFeedId = saveQuestion(authorId);
        long consumerFeedId = saveQuestion(authorId);
        int generation = readyRecommendations(sourceFeedId, 5);
        long usedId = recommendationIds(sourceFeedId).getFirst();

        var consumed = repository.consume(usedId, consumerFeedId, NOW);
        var consumedAgain = repository.consume(usedId, consumerFeedId, NOW);

        assertThat(consumed).hasValueSatisfying(recommendation -> {
            assertThat(recommendation.sourceFeedId()).isEqualTo(sourceFeedId);
            assertThat(recommendation.generation()).isEqualTo(generation);
        });
        assertThat(consumedAgain).isEmpty();
        assertThat(recommendationIds(sourceFeedId)).hasSize(4).doesNotContain(usedId);
        assertThat(repository.findRandomItems(10_000))
                .extracting(QuestionRecommendationItem::recommendationId)
                .doesNotContain(usedId);
        assertThat(repository.findGenerationItems(sourceFeedId, generation))
                .extracting(GenerationItem::consumed)
                .containsExactly(true, false, false, false, false);
    }

    @Test
    void 질문이_수정되어_재생성되면_이전_추천은_사용할_수_없고_대체_생성도_폐기된다() {
        long authorId = insertUser();
        long sourceFeedId = saveQuestion(authorId);
        long consumerFeedId = saveQuestion(authorId);
        int generation = readyRecommendations(sourceFeedId, 5);
        long oldId = recommendationIds(sourceFeedId).getFirst();

        repository.requestGeneration(sourceFeedId, "수정 제목", "수정 본문", UUID.randomUUID(), NOW);

        assertThat(repository.consume(oldId, consumerFeedId, NOW)).isEmpty();
        assertThat(repository.claimReplenish(sourceFeedId, generation, NOW, NOW.plusSeconds(180)).status())
                .isEqualTo(GenerationClaim.ClaimStatus.OBSOLETE);
    }

    @Test
    void 진행_중인_대체_생성_요청은_원글당_하나만_등록된다() {
        long sourceFeedId = saveQuestion(insertUser());
        int generation = readyRecommendations(sourceFeedId, 5);

        repository.requestReplenish(sourceFeedId, generation, UUID.randomUUID(), NOW);
        repository.requestReplenish(sourceFeedId, generation, UUID.randomUUID(), NOW);

        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT count(*) FROM question_recommendation_outbox
                        WHERE feed_id = ? AND kind = 'REPLENISH'
                        """,
                Integer.class,
                sourceFeedId
        );
        assertThat(count).isOne();
    }

    @Test
    void 대체_생성을_선점하면_중복_선점을_막고_추가한_추천은_마지막_순서로_저장된다() {
        long sourceFeedId = saveQuestion(insertUser());
        int generation = readyRecommendations(sourceFeedId, 5);

        ReplenishClaim claim = repository.claimReplenish(sourceFeedId, generation, NOW, NOW.plusSeconds(180));
        ReplenishClaim busy = repository.claimReplenish(sourceFeedId, generation, NOW, NOW.plusSeconds(180));
        boolean appended = repository.appendItems(
                sourceFeedId, generation, List.of(new GeneratedQuestion("대체 추천", "대체 본문")), NOW
        );

        assertThat(claim.status()).isEqualTo(GenerationClaim.ClaimStatus.CLAIMED);
        assertThat(claim.task().replenishedCount()).isZero();
        assertThat(busy.status()).isEqualTo(GenerationClaim.ClaimStatus.BUSY);
        assertThat(appended).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                """
                        SELECT display_order FROM question_recommendation_items
                        WHERE feed_id = ? AND title = '대체 추천'
                        """,
                Integer.class,
                sourceFeedId
        )).isEqualTo(6);
        ReplenishClaim next = repository.claimReplenish(sourceFeedId, generation, NOW, NOW.plusSeconds(180));
        assertThat(next.status()).isEqualTo(GenerationClaim.ClaimStatus.CLAIMED);
        assertThat(next.task().replenishedCount()).isOne();
    }

    @Test
    void 대체_생성_선점을_해제하면_다시_선점할_수_있다() {
        long sourceFeedId = saveQuestion(insertUser());
        int generation = readyRecommendations(sourceFeedId, 5);
        repository.claimReplenish(sourceFeedId, generation, NOW, NOW.plusSeconds(180));

        repository.releaseReplenish(sourceFeedId, generation, null, NOW);

        assertThat(repository.claimReplenish(sourceFeedId, generation, NOW, NOW.plusSeconds(180)).status())
                .isEqualTo(GenerationClaim.ClaimStatus.CLAIMED);
    }

    private int readyRecommendations(long feedId, int count) {
        int generation = repository.requestGeneration(feedId, "질문 제목", "질문 본문", UUID.randomUUID(), NOW);
        repository.claimGeneration(feedId, generation, NOW, NOW.plusSeconds(180));
        List<GeneratedQuestion> questions = IntStream.rangeClosed(1, count)
                .mapToObj(index -> new GeneratedQuestion("추천 " + index, "추천 본문 " + index))
                .toList();
        repository.completeGeneration(feedId, generation, questions, NOW);
        return generation;
    }

    private List<Long> recommendationIds(long feedId) {
        return repository.findByFeedId(feedId).orElseThrow().items().stream()
                .map(QuestionRecommendationItem::recommendationId)
                .toList();
    }

    private long saveQuestion(long authorId) {
        return feedRepository.save(Feed.create(authorId, FeedType.QUESTION, "질문", "질문 본문", false, NOW))
                .getId();
    }

    private long insertUser() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (handle) VALUES (?) RETURNING id",
                Long.class,
                "@rec_" + token
        );
        jdbcTemplate.update(
                "INSERT INTO user_profiles (user_id, display_name, user_type) VALUES (?, ?, ?)",
                userId,
                "추천 테스트",
                "GENERAL"
        );
        return userId;
    }
}

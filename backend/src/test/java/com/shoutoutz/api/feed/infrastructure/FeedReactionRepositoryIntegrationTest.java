package com.shoutoutz.api.feed.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.feed.domain.Feed;
import com.shoutoutz.api.feed.domain.FeedReactionCounts;
import com.shoutoutz.api.feed.domain.FeedReactionRepository;
import com.shoutoutz.api.feed.domain.FeedReactionType;
import com.shoutoutz.api.feed.domain.FeedRepository;
import java.time.Instant;
import java.util.UUID;
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
class FeedReactionRepositoryIntegrationTest {

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private FeedReactionRepository feedReactionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 같은_사용자의_같은_반응을_중복_추가해도_한_건만_저장한다() {
        long authorId = insertUser();
        long otherUserId = insertUser();
        Feed feed = feedRepository.save(Feed.create(
                authorId,
                "피드 제목",
                "피드 본문",
                Instant.parse("2026-09-11T00:00:00Z")
        ));

        feedReactionRepository.add(feed.getId(), authorId, FeedReactionType.LIKE);
        feedReactionRepository.add(feed.getId(), authorId, FeedReactionType.LIKE);
        feedReactionRepository.add(feed.getId(), otherUserId, FeedReactionType.LIKE);
        feedReactionRepository.add(feed.getId(), authorId, FeedReactionType.BOOKMARK);

        assertThat(feedReactionRepository.countByFeedId(feed.getId()))
                .isEqualTo(new FeedReactionCounts(2L, 1L));

        feedReactionRepository.remove(feed.getId(), authorId, FeedReactionType.LIKE);
        feedReactionRepository.remove(feed.getId(), authorId, FeedReactionType.LIKE);

        assertThat(feedReactionRepository.countByFeedId(feed.getId()))
                .isEqualTo(new FeedReactionCounts(1L, 1L));
    }

    private long insertUser() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (handle) VALUES (?) RETURNING id",
                Long.class,
                "reaction_" + token
        );
    }
}

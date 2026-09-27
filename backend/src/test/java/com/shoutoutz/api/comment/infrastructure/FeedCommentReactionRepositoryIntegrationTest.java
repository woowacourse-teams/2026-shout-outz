package com.shoutoutz.api.comment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.comment.domain.FeedCommentReactionCounts;
import com.shoutoutz.api.comment.domain.FeedCommentReactionRepository;
import com.shoutoutz.api.comment.domain.FeedCommentReactionType;
import com.shoutoutz.api.feed.domain.Feed;
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
@SpringBootTest
@Transactional
class FeedCommentReactionRepositoryIntegrationTest {

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private FeedCommentReactionRepository feedCommentReactionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 같은_사용자의_같은_댓글_공감을_중복_추가해도_한_건만_저장한다() {
        long firstUserId = insertUser();
        long secondUserId = insertUser();
        Feed feed = feedRepository.save(Feed.create(
                firstUserId,
                "피드 제목",
                "피드 본문",
                Instant.parse("2026-09-11T00:00:00Z")
        ));
        long commentId = insertComment(feed.getId(), firstUserId);

        feedCommentReactionRepository.add(commentId, firstUserId, FeedCommentReactionType.AGREE);
        feedCommentReactionRepository.add(commentId, firstUserId, FeedCommentReactionType.AGREE);
        feedCommentReactionRepository.add(commentId, secondUserId, FeedCommentReactionType.AGREE);

        assertThat(feedCommentReactionRepository.countByCommentId(commentId))
                .isEqualTo(new FeedCommentReactionCounts(2L));

        assertThat(feedCommentReactionRepository.remove(commentId, firstUserId, FeedCommentReactionType.AGREE))
                .isTrue();
        assertThat(feedCommentReactionRepository.remove(commentId, firstUserId, FeedCommentReactionType.AGREE))
                .isFalse();

        assertThat(feedCommentReactionRepository.countByCommentId(commentId))
                .isEqualTo(new FeedCommentReactionCounts(1L));
    }

    private long insertUser() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (handle) VALUES (?) RETURNING id",
                Long.class,
                "feed_comment_" + token
        );
    }

    private long insertComment(long feedId, long authorId) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO feed_comments (feed_id, author_id, content) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                feedId,
                authorId,
                "공감할 댓글"
        );
    }
}

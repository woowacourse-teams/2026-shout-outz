package com.shoutoutz.api.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.notification.application.NotificationQueryRepository;
import com.shoutoutz.api.notification.application.dto.NotificationPage;
import com.shoutoutz.api.notification.domain.NotificationRepository;
import com.shoutoutz.api.notification.domain.NotificationType;
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
class NotificationRepositoryIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationQueryRepository notificationQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 댓글_생성_알림은_수신자별로_한건만_저장하고_본인은_제외한다() {
        long feedAuthorId = insertUser("feed-author");
        long interestedUserId = insertUser("interested-user");
        long commenterId = insertUser("commenter-user");
        long actorId = insertUser("actor-user");
        long feedId = insertFeed(feedAuthorId);
        insertLike(feedId, interestedUserId);
        insertComment(feedId, commenterId);
        long commentId = insertComment(feedId, actorId);

        notificationRepository.createForFeedComment(feedId, commentId, actorId);
        notificationRepository.createForFeedComment(feedId, commentId, actorId);

        NotificationPage authorPage = notificationQueryRepository.findAll(
                feedAuthorId,
                null,
                20
        );
        NotificationPage interestedPage = notificationQueryRepository.findAll(
                interestedUserId,
                null,
                20
        );
        NotificationPage commenterPage = notificationQueryRepository.findAll(
                commenterId,
                null,
                20
        );
        NotificationPage actorPage = notificationQueryRepository.findAll(actorId, null, 20);

        assertThat(authorPage.items()).extracting(item -> item.notificationType())
                .containsExactly(NotificationType.QUESTION_ACTIVITY);
        assertThat(interestedPage.items()).extracting(item -> item.notificationType())
                .containsExactly(NotificationType.INTERESTED_QUESTION_ACTIVITY);
        assertThat(commenterPage.items()).extracting(item -> item.notificationType())
                .containsExactly(NotificationType.COMMENTED_QUESTION_ACTIVITY);
        assertThat(actorPage.items()).isEmpty();
        assertThat(authorPage.totalCount()).isEqualTo(1L);
    }

    @Test
    void 읽음_처리와_읽지_않은_수_조회가_동작한다() {
        long recipientId = insertUser("notification-recipient");
        long actorId = insertUser("notification-actor");
        long feedId = insertFeed(recipientId);
        long commentId = insertComment(feedId, actorId);

        notificationRepository.createForFeedComment(feedId, commentId, actorId);
        long notificationId = jdbcTemplate.queryForObject(
                "SELECT id FROM notifications WHERE recipient_id = ?",
                Long.class,
                recipientId
        );

        assertThat(notificationQueryRepository.countUnread(recipientId)).isEqualTo(1L);
        assertThat(notificationRepository.markRead(recipientId, notificationId)).isTrue();
        assertThat(notificationQueryRepository.countUnread(recipientId)).isZero();
        assertThat(notificationRepository.markRead(recipientId, notificationId)).isTrue();
    }

    private long insertUser(String label) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (handle) VALUES (?) RETURNING id",
                Long.class,
                label.substring(0, Math.min(label.length(), 20)) + "-" + suffix
        );
        jdbcTemplate.update(
                "INSERT INTO user_profiles (user_id, display_name) VALUES (?, ?)",
                userId,
                label
        );
        return userId;
    }

    private long insertFeed(long authorId) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO feeds (author_id, title, content) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                authorId,
                "질문 제목",
                "질문 내용"
        );
    }

    private void insertLike(long feedId, long userId) {
        jdbcTemplate.update(
                "INSERT INTO feed_reactions (feed_id, user_id, reaction_type) VALUES (?, ?, 'LIKE')",
                feedId,
                userId
        );
    }

    private long insertComment(long feedId, long authorId) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO feed_comments (feed_id, author_id, content) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                feedId,
                authorId,
                "댓글 내용"
        );
    }
}

package com.shoutoutz.api.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.feed.domain.FeedType;
import com.shoutoutz.api.notification.application.NotificationQueryRepository;
import com.shoutoutz.api.notification.application.dto.NotificationPage;
import com.shoutoutz.api.notification.domain.NotificationRepository;
import com.shoutoutz.api.notification.domain.NotificationType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

    @ParameterizedTest
    @EnumSource(FeedType.class)
    void 글_유형에_따라_작성자와_관심_사용자와_댓글_참여자에게_알림을_보낸다(FeedType feedType) {
        long feedAuthorId = insertUser("feed-author");
        long interestedUserId = insertUser("interested-user");
        long commenterId = insertUser("commenter-user");
        long actorId = insertUser("actor-user");
        long feedId = insertFeed(feedAuthorId, feedType);
        insertLike(feedId, feedAuthorId);
        insertComment(feedId, feedAuthorId);
        insertLike(feedId, interestedUserId);
        insertComment(feedId, commenterId);
        insertLike(feedId, actorId);
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

        boolean question = feedType == FeedType.QUESTION;
        assertThat(authorPage.items()).extracting(item -> item.notificationType())
                .containsExactly(question ? NotificationType.QUESTION_ACTIVITY : NotificationType.POST_ACTIVITY);
        assertThat(authorPage.items()).extracting(item -> item.message())
                .containsExactly(question
                        ? "내 질문에 새로운 답변이 달렸어요."
                        : "내 피드에 새로운 댓글이 달렸어요.");
        if (question) {
            assertThat(interestedPage.items()).extracting(item -> item.notificationType())
                    .containsExactly(NotificationType.INTERESTED_QUESTION_ACTIVITY);
            assertThat(interestedPage.items()).extracting(item -> item.message())
                    .containsExactly("관심 있는 질문에 새로운 답변이 달렸어요.");
        } else {
            assertThat(interestedPage.items()).isEmpty();
        }
        assertThat(commenterPage.items()).extracting(item -> item.notificationType())
                .containsExactly(question
                        ? NotificationType.COMMENTED_QUESTION_ACTIVITY
                        : NotificationType.COMMENTED_POST_ACTIVITY);
        assertThat(commenterPage.items()).extracting(item -> item.message())
                .containsExactly(question
                        ? "댓글을 남긴 질문에 새로운 답변이 달렸어요."
                        : "댓글을 남긴 피드에 새로운 댓글이 달렸어요.");
        assertThat(actorPage.items()).isEmpty();
        assertThat(authorPage.totalCount()).isEqualTo(1L);
    }

    @Test
    void 질문_수신_조건이_겹치면_작성자와_관심_사용자_순서로_한건만_보낸다() {
        long authorId = insertUser("author");
        long interestedId = insertUser("interested");
        long actorId = insertUser("actor");
        long feedId = insertFeed(authorId, FeedType.QUESTION);
        insertLike(feedId, authorId);
        insertComment(feedId, authorId);
        insertLike(feedId, interestedId);
        insertComment(feedId, interestedId);
        insertLike(feedId, actorId);
        long commentId = insertComment(feedId, actorId);

        notificationRepository.createForFeedComment(feedId, commentId, actorId);

        assertThat(notificationQueryRepository.findAll(authorId, null, 20).items())
                .extracting(item -> item.notificationType())
                .containsExactly(NotificationType.QUESTION_ACTIVITY);
        assertThat(notificationQueryRepository.findAll(interestedId, null, 20).items())
                .extracting(item -> item.notificationType())
                .containsExactly(NotificationType.INTERESTED_QUESTION_ACTIVITY);
        assertThat(notificationQueryRepository.findAll(actorId, null, 20).items()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(FeedType.class)
    void 대댓글도_글_작성자와_기존_댓글_작성자에게_알림을_보낸다(FeedType feedType) {
        long authorId = insertUser("author");
        long commenterId = insertUser("commenter");
        long actorId = insertUser("actor");
        long feedId = insertFeed(authorId, feedType);
        long parentId = insertComment(feedId, commenterId);
        long replyId = jdbcTemplate.queryForObject(
                "INSERT INTO feed_comments (feed_id, author_id, parent_id, content) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, feedId, actorId, parentId, "대댓글 내용"
        );

        notificationRepository.createForFeedComment(feedId, replyId, actorId);

        assertThat(notificationQueryRepository.findAll(authorId, null, 20).items())
                .extracting(item -> item.commentId()).containsExactly(replyId);
        assertThat(notificationQueryRepository.findAll(commenterId, null, 20).items())
                .extracting(item -> item.commentId()).containsExactly(replyId);
        assertThat(notificationQueryRepository.findAll(actorId, null, 20).items()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(FeedType.class)
    void 삭제된_댓글의_작성자와_북마크만_한_사용자는_알림을_받지_않는다(FeedType feedType) {
        long authorId = insertUser("author");
        long commenterId = insertUser("commenter");
        long bookmarkerId = insertUser("bookmarker");
        long actorId = insertUser("actor");
        long feedId = insertFeed(authorId, feedType);
        long deletedCommentId = insertComment(feedId, commenterId);
        jdbcTemplate.update("UPDATE feed_comments SET deleted_at = now() WHERE id = ?", deletedCommentId);
        jdbcTemplate.update(
                "INSERT INTO feed_reactions (feed_id, user_id, reaction_type) VALUES (?, ?, 'BOOKMARK')",
                feedId, bookmarkerId
        );
        long commentId = insertComment(feedId, actorId);

        notificationRepository.createForFeedComment(feedId, commentId, actorId);

        assertThat(notificationQueryRepository.findAll(commenterId, null, 20).items()).isEmpty();
        assertThat(notificationQueryRepository.findAll(bookmarkerId, null, 20).items()).isEmpty();
    }

    @Test
    void 읽음_처리와_읽지_않은_수_조회가_동작한다() {
        long recipientId = insertUser("notification-recipient");
        long actorId = insertUser("notification-actor");
        long feedId = insertFeed(recipientId, FeedType.POST);
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

    private long insertFeed(long authorId, FeedType feedType) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO feeds (author_id, feed_type, title, content) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                authorId,
                feedType.name(),
                "글 제목",
                "글 내용"
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

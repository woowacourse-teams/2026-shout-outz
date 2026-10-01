package com.shoutoutz.api.notification.infrastructure;

import com.shoutoutz.api.notification.application.NotificationQueryRepository;
import com.shoutoutz.api.notification.application.dto.NotificationCursor;
import com.shoutoutz.api.notification.application.dto.NotificationItem;
import com.shoutoutz.api.notification.application.dto.NotificationPage;
import com.shoutoutz.api.notification.domain.NotificationRepository;
import com.shoutoutz.api.notification.domain.NotificationType;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository, NotificationQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public NotificationPage findAll(long recipientId, NotificationCursor cursor, int size) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("recipientId", recipientId)
                .addValue("hasCursor", cursor != null)
                .addValue(
                        "cursorCreatedAt",
                        cursor == null
                                ? Timestamp.from(java.time.Instant.EPOCH)
                                : Timestamp.from(cursor.createdAt()),
                        Types.TIMESTAMP
                )
                .addValue("cursorId", cursor == null ? Long.MAX_VALUE : cursor.notificationId())
                .addValue("limit", size + 1);

        List<NotificationItem> queriedItems = jdbcTemplate.query(
                """
                        SELECT n.id AS notification_id,
                               n.notification_type,
                               n.message,
                               n.feed_id,
                               f.title AS feed_title,
                               n.comment_id,
                               u.id AS actor_id,
                               u.handle AS actor_handle,
                               up.display_name AS actor_display_name,
                               up.avatar_image_id AS actor_avatar_image_id,
                               n.is_read,
                               n.created_at
                        FROM notifications n
                        LEFT JOIN feeds f
                          ON f.id = n.feed_id
                         AND f.deleted_at IS NULL
                        LEFT JOIN users u
                          ON u.id = n.actor_id
                         AND u.status = 'ACTIVE'
                         AND u.deleted_at IS NULL
                        LEFT JOIN user_profiles up ON up.user_id = u.id
                        WHERE n.recipient_id = :recipientId
                          AND (
                              :hasCursor = FALSE
                              OR n.created_at < :cursorCreatedAt
                              OR (
                                  n.created_at = :cursorCreatedAt
                                  AND n.id < :cursorId
                              )
                          )
                        ORDER BY n.created_at DESC, n.id DESC
                        LIMIT :limit
                        """,
                parameters,
                (resultSet, rowNumber) -> new NotificationItem(
                        resultSet.getLong("notification_id"),
                        NotificationType.valueOf(resultSet.getString("notification_type")),
                        resultSet.getString("message"),
                        resultSet.getObject("feed_id", Long.class),
                        resultSet.getString("feed_title"),
                        resultSet.getObject("comment_id", Long.class),
                        resultSet.getObject("actor_id", Long.class),
                        resultSet.getString("actor_handle"),
                        resultSet.getString("actor_display_name"),
                        resultSet.getObject("actor_avatar_image_id", Long.class),
                        resultSet.getBoolean("is_read"),
                        resultSet.getTimestamp("created_at").toInstant()
                )
        );

        boolean hasNext = queriedItems.size() > size;
        List<NotificationItem> items = queriedItems.stream()
                .limit(size)
                .toList();
        return new NotificationPage(items, hasNext, countAll(recipientId));
    }

    @Override
    public long countUnread(long recipientId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM notifications
                        WHERE recipient_id = :recipientId
                          AND is_read = FALSE
                        """,
                new MapSqlParameterSource("recipientId", recipientId),
                Long.class
        );
    }

    @Override
    public void createForFeedComment(long feedId, long commentId, long actorId) {
        jdbcTemplate.update(
                """
                        WITH candidates AS (
                            SELECT f.author_id AS recipient_id,
                                   'QUESTION_ACTIVITY' AS notification_type,
                                   '내 질문에 새로운 답변이 달렸어요.' AS message,
                                   1 AS priority
                            FROM feeds f
                            JOIN users recipient ON recipient.id = f.author_id
                            WHERE f.id = :feedId
                              AND f.deleted_at IS NULL
                              AND recipient.status = 'ACTIVE'
                              AND recipient.deleted_at IS NULL

                            UNION ALL

                            SELECT reaction.user_id AS recipient_id,
                                   'INTERESTED_QUESTION_ACTIVITY' AS notification_type,
                                   '관심 있는 질문에 새로운 답변이 달렸어요.' AS message,
                                   2 AS priority
                            FROM feed_reactions reaction
                            JOIN users recipient ON recipient.id = reaction.user_id
                            WHERE reaction.feed_id = :feedId
                              AND reaction.reaction_type = 'LIKE'
                              AND recipient.status = 'ACTIVE'
                              AND recipient.deleted_at IS NULL

                            UNION ALL

                            SELECT comment.author_id AS recipient_id,
                                   'COMMENTED_QUESTION_ACTIVITY' AS notification_type,
                                   '참여한 질문에 새로운 답변이 달렸어요.' AS message,
                                   3 AS priority
                            FROM feed_comments comment
                            JOIN users recipient ON recipient.id = comment.author_id
                            WHERE comment.feed_id = :feedId
                              AND comment.deleted_at IS NULL
                              AND recipient.status = 'ACTIVE'
                              AND recipient.deleted_at IS NULL
                        ), deduplicated AS (
                            SELECT DISTINCT ON (recipient_id)
                                   recipient_id,
                                   notification_type,
                                   message
                            FROM candidates
                            WHERE recipient_id <> :actorId
                            ORDER BY recipient_id, priority
                        )
                        INSERT INTO notifications (
                            recipient_id,
                            actor_id,
                            feed_id,
                            comment_id,
                            notification_type,
                            message
                        )
                        SELECT recipient_id,
                               :actorId,
                               :feedId,
                               :commentId,
                               notification_type,
                               message
                        FROM deduplicated
                        ON CONFLICT DO NOTHING
                        """,
                new MapSqlParameterSource()
                        .addValue("feedId", feedId)
                        .addValue("commentId", commentId)
                        .addValue("actorId", actorId)
        );
    }

    @Override
    public boolean markRead(long recipientId, long notificationId) {
        return jdbcTemplate.update(
                """
                        UPDATE notifications
                        SET is_read = TRUE
                        WHERE id = :notificationId
                          AND recipient_id = :recipientId
                        """,
                new MapSqlParameterSource()
                        .addValue("notificationId", notificationId)
                        .addValue("recipientId", recipientId)
        ) == 1;
    }

    @Override
    public void markAllRead(long recipientId) {
        jdbcTemplate.update(
                """
                        UPDATE notifications
                        SET is_read = TRUE
                        WHERE recipient_id = :recipientId
                          AND is_read = FALSE
                        """,
                new MapSqlParameterSource("recipientId", recipientId)
        );
    }

    private long countAll(long recipientId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM notifications
                        WHERE recipient_id = :recipientId
                        """,
                new MapSqlParameterSource("recipientId", recipientId),
                Long.class
        );
    }
}

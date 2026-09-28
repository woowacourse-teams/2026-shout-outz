package com.shoutoutz.api.comment.infrastructure;

import com.shoutoutz.api.comment.application.UserCommentQueryRepository;
import com.shoutoutz.api.comment.application.dto.UserCommentCursor;
import com.shoutoutz.api.comment.application.dto.UserCommentItem;
import com.shoutoutz.api.comment.application.dto.UserCommentPage;
import com.shoutoutz.api.comment.application.dto.UserCommentType;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserCommentQueryRepositoryImpl implements UserCommentQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public UserCommentPage findAllByAuthorId(
            long authorId,
            UserCommentCursor cursor,
            int size
    ) {
        return findAllByAuthorId(authorId, authorId, cursor, size);
    }

    @Override
    public UserCommentPage findAllByAuthorId(
            long authorId,
            Long viewerId,
            UserCommentCursor cursor,
            int size
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT comment_id,
                       comment_type,
                       target_id,
                       content,
                       created_at,
                       updated_at,
                       agree_count,
                       agreed_by_me
                FROM (
                    SELECT fc.id AS comment_id,
                           'FEED' AS comment_type,
                           1 AS type_order,
                           fc.feed_id AS target_id,
                           fc.content,
                           fc.created_at,
                           fc.updated_at,
                           (
                               SELECT COUNT(*)
                               FROM feed_comment_reactions r
                               WHERE r.comment_id = fc.id
                                 AND r.reaction_type = 'AGREE'
                           ) AS agree_count,
                           EXISTS (
                               SELECT 1
                               FROM feed_comment_reactions r
                               WHERE r.comment_id = fc.id
                                 AND r.user_id = :viewerId
                                 AND r.reaction_type = 'AGREE'
                           ) AS agreed_by_me
                    FROM feed_comments fc
                    JOIN feeds f ON f.id = fc.feed_id
                    WHERE fc.author_id = :authorId
                      AND fc.deleted_at IS NULL
                      AND f.deleted_at IS NULL

                    UNION ALL

                    SELECT pc.id AS comment_id,
                           'PROJECT' AS comment_type,
                           2 AS type_order,
                           pc.project_id AS target_id,
                           pc.content,
                           pc.created_at,
                           pc.updated_at,
                           (
                               SELECT COUNT(*)
                               FROM project_comment_reactions r
                               WHERE r.comment_id = pc.id
                                 AND r.reaction_type = 'AGREE'
                           ) AS agree_count,
                           EXISTS (
                               SELECT 1
                               FROM project_comment_reactions r
                               WHERE r.comment_id = pc.id
                                 AND r.user_id = :viewerId
                                 AND r.reaction_type = 'AGREE'
                           ) AS agreed_by_me
                    FROM project_comments pc
                    JOIN projects p ON p.id = pc.project_id
                    WHERE pc.author_id = :authorId
                      AND pc.deleted_at IS NULL
                      AND p.deleted_at IS NULL
                      AND p.approval_status = 'APPROVED'
                ) comments
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("authorId", authorId)
                .addValue("viewerId", viewerId, Types.BIGINT)
                .addValue("limit", size + 1);

        if (cursor != null) {
            sql.append("""
                    WHERE (created_at, type_order, comment_id)
                        < (:cursorCreatedAt, :cursorTypeOrder, :cursorCommentId)
                    """);
            parameters
                    .addValue("cursorCreatedAt", Timestamp.from(cursor.createdAt()))
                    .addValue("cursorTypeOrder", cursor.type().cursorOrder())
                    .addValue("cursorCommentId", cursor.commentId());
        }
        sql.append("""
                ORDER BY created_at DESC, type_order DESC, comment_id DESC
                LIMIT :limit
                """);

        List<UserCommentItem> items = jdbcTemplate.query(
                sql.toString(),
                parameters,
                (resultSet, rowNumber) -> new UserCommentItem(
                        resultSet.getLong("comment_id"),
                        UserCommentType.valueOf(resultSet.getString("comment_type")),
                        resultSet.getLong("target_id"),
                        resultSet.getString("content"),
                        resultSet.getTimestamp("created_at").toInstant(),
                        resultSet.getTimestamp("updated_at").toInstant(),
                        resultSet.getLong("agree_count"),
                        resultSet.getBoolean("agreed_by_me")
                )
        );
        return createPage(items, size, countAllByAuthorId(authorId));
    }

    private long countAllByAuthorId(long authorId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT
                            (
                                SELECT COUNT(*)
                                FROM feed_comments fc
                                JOIN feeds f ON f.id = fc.feed_id
                                WHERE fc.author_id = :authorId
                                  AND fc.deleted_at IS NULL
                                  AND f.deleted_at IS NULL
                            ) + (
                                SELECT COUNT(*)
                                FROM project_comments pc
                                JOIN projects p ON p.id = pc.project_id
                                WHERE pc.author_id = :authorId
                                  AND pc.deleted_at IS NULL
                                  AND p.deleted_at IS NULL
                                  AND p.approval_status = 'APPROVED'
                            )
                        """,
                new MapSqlParameterSource("authorId", authorId),
                Long.class
        );
    }

    private UserCommentPage createPage(List<UserCommentItem> items, int size, long totalCount) {
        if (items.size() <= size) {
            return new UserCommentPage(items, false, totalCount);
        }
        return new UserCommentPage(items.subList(0, size), true, totalCount);
    }
}

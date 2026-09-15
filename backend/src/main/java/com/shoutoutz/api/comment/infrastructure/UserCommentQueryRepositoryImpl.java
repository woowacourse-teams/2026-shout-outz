package com.shoutoutz.api.comment.infrastructure;

import com.shoutoutz.api.comment.application.UserCommentQueryRepository;
import com.shoutoutz.api.comment.application.dto.UserCommentCursor;
import com.shoutoutz.api.comment.application.dto.UserCommentItem;
import com.shoutoutz.api.comment.application.dto.UserCommentType;
import java.sql.Timestamp;
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
    public List<UserCommentItem> findAllByAuthorId(
            long authorId,
            UserCommentCursor cursor,
            int limit
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT comment_id,
                       comment_type,
                       target_id,
                       content,
                       created_at,
                       updated_at
                FROM (
                    SELECT fc.id AS comment_id,
                           'FEED' AS comment_type,
                           1 AS type_order,
                           fc.feed_id AS target_id,
                           fc.content,
                           fc.created_at,
                           fc.updated_at
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
                           pc.updated_at
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
                .addValue("limit", limit);

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

        return jdbcTemplate.query(
                sql.toString(),
                parameters,
                (resultSet, rowNumber) -> new UserCommentItem(
                        resultSet.getLong("comment_id"),
                        UserCommentType.valueOf(resultSet.getString("comment_type")),
                        resultSet.getLong("target_id"),
                        resultSet.getString("content"),
                        resultSet.getTimestamp("created_at").toInstant(),
                        resultSet.getTimestamp("updated_at").toInstant()
                )
        );
    }
}

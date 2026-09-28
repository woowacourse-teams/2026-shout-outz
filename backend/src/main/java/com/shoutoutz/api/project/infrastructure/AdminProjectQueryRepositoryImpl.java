package com.shoutoutz.api.project.infrastructure;

import com.shoutoutz.api.project.application.AdminProjectQueryRepository;
import com.shoutoutz.api.project.application.dto.AdminProjectCursor;
import com.shoutoutz.api.project.application.dto.AdminProjectItem;
import com.shoutoutz.api.project.application.dto.AdminProjectPage;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.ServiceStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminProjectQueryRepositoryImpl implements AdminProjectQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public AdminProjectPage findAll(ApprovalStatus status, AdminProjectCursor cursor, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    p.id,
                    p.slug,
                    p.title,
                    p.team_name,
                    p.tagline,
                    p.cohort,
                    p.service_status,
                    p.approval_status,
                    p.thumbnail_media_id,
                    p.registered_by,
                    p.star_count,
                    p.created_at,
                    p.updated_at,
                    CASE
                        WHEN p.approval_status = 'REJECTED' THEN (
                            SELECT h.reason
                            FROM project_approval_histories h
                            WHERE h.project_id = p.id
                              AND h.to_status = 'REJECTED'
                            ORDER BY h.changed_at DESC, h.id DESC
                            LIMIT 1
                        )
                    END AS reject_reason,
                    (
                        SELECT COUNT(*)
                        FROM project_reactions r
                        WHERE r.project_id = p.id
                          AND r.reaction_type = 'LIKE'
                    ) AS like_count,
                    (
                        SELECT COUNT(*)
                        FROM project_reactions r
                        WHERE r.project_id = p.id
                          AND r.reaction_type = 'BOOKMARK'
                    ) AS bookmark_count,
                    (
                        SELECT COUNT(*)
                        FROM project_comments c
                        WHERE c.project_id = p.id
                          AND c.deleted_at IS NULL
                    ) AS comment_count
                FROM projects p
                WHERE p.deleted_at IS NULL
                  AND p.approval_status = :status
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("status", status.name())
                .addValue("limit", size + 1);

        if (cursor != null) {
            sql.append("AND (p.created_at, p.id) < (:cursorCreatedAt, :cursorProjectId)\n");
            parameters
                    .addValue(
                            "cursorCreatedAt",
                            OffsetDateTime.ofInstant(cursor.createdAt(), ZoneOffset.UTC)
                    )
                    .addValue("cursorProjectId", cursor.projectId());
        }
        sql.append("ORDER BY p.created_at DESC, p.id DESC\nLIMIT :limit");

        List<AdminProjectItem> fetched = jdbcTemplate.query(
                sql.toString(),
                parameters,
                (resultSet, rowNumber) -> new AdminProjectItem(
                        resultSet.getLong("id"),
                        resultSet.getString("slug"),
                        resultSet.getString("title"),
                        resultSet.getString("team_name"),
                        resultSet.getString("tagline"),
                        resultSet.getInt("cohort"),
                        ServiceStatus.valueOf(resultSet.getString("service_status")),
                        ApprovalStatus.valueOf(resultSet.getString("approval_status")),
                        resultSet.getString("reject_reason"),
                        resultSet.getObject("thumbnail_media_id", Long.class),
                        resultSet.getObject("registered_by", Long.class),
                        resultSet.getObject("star_count", Integer.class),
                        resultSet.getLong("like_count"),
                        resultSet.getLong("comment_count"),
                        resultSet.getLong("bookmark_count"),
                        List.of(),
                        List.of(),
                        resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
                        resultSet.getObject("updated_at", OffsetDateTime.class).toInstant()
                )
        );
        Long totalCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM projects
                        WHERE deleted_at IS NULL
                          AND approval_status = :status
                        """,
                new MapSqlParameterSource("status", status.name()),
                Long.class
        );

        boolean hasNext = fetched.size() > size;
        List<AdminProjectItem> items = hasNext ? fetched.subList(0, size) : fetched;
        return new AdminProjectPage(items, hasNext, totalCount == null ? 0L : totalCount);
    }
}

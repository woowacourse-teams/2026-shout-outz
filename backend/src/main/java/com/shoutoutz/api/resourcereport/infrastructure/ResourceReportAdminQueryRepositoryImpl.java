package com.shoutoutz.api.resourcereport.infrastructure;

import com.shoutoutz.api.resourcereport.application.ResourceReportAdminQueryRepository;
import com.shoutoutz.api.resourcereport.application.dto.ResourceReportAdminCursor;
import com.shoutoutz.api.resourcereport.application.dto.ResourceReportAdminItem;
import com.shoutoutz.api.resourcereport.application.dto.ResourceReportAdminPage;
import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;
import com.shoutoutz.api.resourcereport.domain.ResourceReportStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ResourceReportAdminQueryRepositoryImpl implements ResourceReportAdminQueryRepository {

    /**
     * 필요 없어요 제보는 지금 자료를 (source, external_key) 로 찾아 붙인다. 자료가 내려갔으면 자료 칸은 모두 null 이다.
     */
    private static final String FIND_ALL_SELECT_SQL = """
            SELECT report.id,
                   report.kind,
                   report.url,
                   report.memo,
                   report.user_id,
                   report.status,
                   report.created_at,
                   project.slug AS project_slug,
                   project.title AS project_title,
                   document.id AS resource_id,
                   document.source AS resource_source,
                   document.title AS resource_title,
                   document.url AS resource_url
            FROM resource_reports report
            JOIN projects project ON project.id = report.project_id
            LEFT JOIN resource_documents document
                   ON report.kind = 'REMOVE'
                  AND document.source = report.resource_source
                  AND document.external_key = report.resource_external_key
                  AND document.deleted_at IS NULL
            WHERE 1 = 1
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public ResourceReportAdminPage findAll(
            ResourceReportStatus status,
            ResourceReportKind kind,
            ResourceReportAdminCursor cursor,
            int size
    ) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("limit", size + 1);
        String filter = filter(parameters, status, kind);
        StringBuilder sql = new StringBuilder(FIND_ALL_SELECT_SQL).append(filter);
        if (cursor != null) {
            sql.append("AND (report.created_at, report.id) < (:cursorCreatedAt, :cursorReportId)\n");
            parameters
                    .addValue("cursorCreatedAt", OffsetDateTime.ofInstant(cursor.createdAt(), ZoneOffset.UTC))
                    .addValue("cursorReportId", cursor.reportId());
        }
        sql.append("ORDER BY report.created_at DESC, report.id DESC\n");
        sql.append("LIMIT :limit");

        List<ResourceReportAdminItem> reports = jdbcTemplate.query(
                sql.toString(),
                parameters,
                (resultSet, rowNumber) -> toAdminItem(resultSet)
        );
        long totalCount = countAll(filter, parameters);
        if (reports.size() <= size) {
            return new ResourceReportAdminPage(reports, false, totalCount);
        }
        return new ResourceReportAdminPage(reports.subList(0, size), true, totalCount);
    }

    private static String filter(
            MapSqlParameterSource parameters,
            ResourceReportStatus status,
            ResourceReportKind kind
    ) {
        StringBuilder filter = new StringBuilder();
        if (status != null) {
            filter.append("AND report.status = :status\n");
            parameters.addValue("status", status.name());
        }
        if (kind != null) {
            filter.append("AND report.kind = :kind\n");
            parameters.addValue("kind", kind.name());
        }
        return filter.toString();
    }

    /**
     * totalCount 는 커서와 상관없이 상태, 종류 조건에 맞는 전체 제보 수다.
     */
    private long countAll(String filter, MapSqlParameterSource parameters) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM resource_reports report WHERE 1 = 1\n" + filter,
                parameters,
                Long.class
        );
        return count == null ? 0 : count;
    }

    private static ResourceReportAdminItem toAdminItem(ResultSet resultSet) throws SQLException {
        return new ResourceReportAdminItem(
                resultSet.getLong("id"),
                ResourceReportKind.valueOf(resultSet.getString("kind")),
                resultSet.getString("project_slug"),
                resultSet.getString("project_title"),
                resultSet.getObject("resource_id", Long.class),
                resultSet.getString("resource_source"),
                resultSet.getString("resource_title"),
                resultSet.getString("resource_url"),
                resultSet.getString("url"),
                resultSet.getString("memo"),
                resultSet.getLong("user_id"),
                ResourceReportStatus.valueOf(resultSet.getString("status")),
                toInstant(resultSet, "created_at")
        );
    }

    private static Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}

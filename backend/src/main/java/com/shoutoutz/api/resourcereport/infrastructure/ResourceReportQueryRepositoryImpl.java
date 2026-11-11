package com.shoutoutz.api.resourcereport.infrastructure;

import com.shoutoutz.api.resourcereport.application.ResourceReportQueryRepository;
import com.shoutoutz.api.resourcereport.application.dto.MyResourceReport;
import com.shoutoutz.api.resourcereport.application.dto.ReportableResource;
import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;
import com.shoutoutz.api.resourcereport.domain.ResourceReportStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ResourceReportQueryRepositoryImpl implements ResourceReportQueryRepository {

    private static final String IS_PROJECT_MEMBER_SQL = """
            SELECT EXISTS (
                       SELECT 1
                       FROM project_members member
                       WHERE member.project_id = :projectId
                         AND member.user_id = :userId
                   )
                OR EXISTS (
                       SELECT 1
                       FROM woowa_archived_project_members archived
                       WHERE archived.project_id = :projectId
                         AND archived.matched_user_id = :userId
                   )
            """;

    private static final String FIND_RESOURCES_SQL = """
            SELECT id, project_id, source, external_key
            FROM resource_documents
            WHERE id IN (:resourceIds)
              AND deleted_at IS NULL
            """;

    /**
     * 필요 없어요 제보는 지금 자료를 (source, external_key) 로 찾아 id 를 붙인다. 자료가 내려갔으면 id 는 null 이다.
     */
    private static final String FIND_MINE_SQL = """
            SELECT report.id,
                   report.kind,
                   report.url,
                   report.status,
                   report.created_at,
                   document.id AS resource_id
            FROM resource_reports report
            LEFT JOIN resource_documents document
                   ON report.kind = 'REMOVE'
                  AND document.source = report.resource_source
                  AND document.external_key = report.resource_external_key
                  AND document.deleted_at IS NULL
            WHERE report.project_id = :projectId
              AND report.user_id = :userId
            ORDER BY report.created_at DESC, report.id DESC
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public boolean isProjectMember(long projectId, long userId) {
        Boolean member = jdbcTemplate.queryForObject(
                IS_PROJECT_MEMBER_SQL,
                new MapSqlParameterSource()
                        .addValue("projectId", projectId)
                        .addValue("userId", userId),
                Boolean.class
        );
        return Boolean.TRUE.equals(member);
    }

    @Override
    public Map<Long, ReportableResource> findResources(Collection<Long> resourceIds) {
        if (resourceIds.isEmpty()) {
            return Map.of();
        }
        List<ReportableResource> resources = jdbcTemplate.query(
                FIND_RESOURCES_SQL,
                new MapSqlParameterSource("resourceIds", resourceIds),
                (resultSet, rowNumber) -> new ReportableResource(
                        resultSet.getLong("id"),
                        resultSet.getObject("project_id", Long.class),
                        resultSet.getString("source"),
                        resultSet.getString("external_key")
                )
        );
        return resources.stream()
                .collect(Collectors.toMap(ReportableResource::resourceId, Function.identity()));
    }

    @Override
    public List<MyResourceReport> findMine(long projectId, long userId) {
        return jdbcTemplate.query(
                FIND_MINE_SQL,
                new MapSqlParameterSource()
                        .addValue("projectId", projectId)
                        .addValue("userId", userId),
                (resultSet, rowNumber) -> new MyResourceReport(
                        resultSet.getLong("id"),
                        ResourceReportKind.valueOf(resultSet.getString("kind")),
                        resultSet.getObject("resource_id", Long.class),
                        resultSet.getString("url"),
                        ResourceReportStatus.valueOf(resultSet.getString("status")),
                        toInstant(resultSet, "created_at")
                )
        );
    }

    private static Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}

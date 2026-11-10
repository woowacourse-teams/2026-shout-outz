package com.shoutoutz.api.resourcereport.infrastructure;

import com.shoutoutz.api.resourcereport.domain.ResourceReport;
import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;
import com.shoutoutz.api.resourcereport.domain.ResourceReportRepository;
import com.shoutoutz.api.resourcereport.domain.ResourceReportStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 자료 제보 저장소.
 * 같은 제보를 두 번 보내면(버튼 두 번 누름 포함) 고유 인덱스에 걸리는 행을 새로 만들지 않고 기존 행의 id 를 돌려줘야 해서,
 * 실패한 INSERT 가 트랜잭션을 깨뜨리지 않는 ON CONFLICT DO NOTHING 을 쓰려고 JDBC 로 다룬다.
 */
@Repository
@RequiredArgsConstructor
public class ResourceReportRepositoryImpl implements ResourceReportRepository {

    private static final String INSERT_SQL = """
            INSERT INTO resource_reports (project_id, user_id, kind, resource_source, resource_external_key, url, memo,
                                          status, created_at, updated_at)
            VALUES (:projectId, :userId, :kind, :resourceSource, :resourceExternalKey, :url, :memo,
                    :status, :createdAt, :updatedAt)
            ON CONFLICT DO NOTHING
            RETURNING id
            """;

    private static final String FIND_REMOVAL_ID_SQL = """
            SELECT id
            FROM resource_reports
            WHERE kind = 'REMOVE'
              AND user_id = :userId
              AND resource_source = :resourceSource
              AND resource_external_key = :resourceExternalKey
            """;

    private static final String FIND_ADDITION_ID_SQL = """
            SELECT id
            FROM resource_reports
            WHERE kind = 'ADD'
              AND user_id = :userId
              AND url = :url
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT id,
                   project_id,
                   user_id,
                   kind,
                   resource_source,
                   resource_external_key,
                   url,
                   memo,
                   status,
                   created_at,
                   updated_at,
                   handled_at,
                   handled_by
            FROM resource_reports
            WHERE id = :id
            """;

    private static final String UPDATE_STATUS_SQL = """
            UPDATE resource_reports
            SET status = :status,
                handled_by = :handledBy,
                handled_at = :handledAt,
                updated_at = :updatedAt
            WHERE id = :id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public long saveIfAbsent(ResourceReport report) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("projectId", report.getProjectId())
                .addValue("userId", report.getUserId())
                .addValue("kind", report.getKind().name())
                .addValue("resourceSource", report.getResourceSource(), Types.VARCHAR)
                .addValue("resourceExternalKey", report.getResourceExternalKey(), Types.VARCHAR)
                .addValue("url", report.getUrl(), Types.VARCHAR)
                .addValue("memo", report.getMemo(), Types.VARCHAR)
                .addValue("status", report.getStatus().name())
                .addValue("createdAt", toOffsetDateTime(report.getCreatedAt()))
                .addValue("updatedAt", toOffsetDateTime(report.getUpdatedAt()));
        List<Long> insertedIds = jdbcTemplate.queryForList(INSERT_SQL, parameters, Long.class);
        if (!insertedIds.isEmpty()) {
            return insertedIds.getFirst();
        }
        String findExistingSql = report.getKind() == ResourceReportKind.REMOVE
                ? FIND_REMOVAL_ID_SQL
                : FIND_ADDITION_ID_SQL;
        return jdbcTemplate.queryForObject(findExistingSql, parameters, Long.class);
    }

    @Override
    public Optional<ResourceReport> findById(long reportId) {
        return jdbcTemplate.query(
                FIND_BY_ID_SQL,
                new MapSqlParameterSource("id", reportId),
                (resultSet, rowNumber) -> toDomain(resultSet)
        ).stream().findFirst();
    }

    @Override
    public ResourceReport updateStatus(ResourceReport report) {
        jdbcTemplate.update(UPDATE_STATUS_SQL, new MapSqlParameterSource()
                .addValue("id", report.getId())
                .addValue("status", report.getStatus().name())
                .addValue("handledBy", report.getHandledByUserId(), Types.BIGINT)
                .addValue("handledAt", toOffsetDateTime(report.getHandledAt()), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("updatedAt", toOffsetDateTime(report.getUpdatedAt())));
        return report;
    }

    private static ResourceReport toDomain(ResultSet resultSet) throws SQLException {
        return ResourceReport.reconstitute(
                resultSet.getLong("id"),
                resultSet.getLong("project_id"),
                resultSet.getLong("user_id"),
                ResourceReportKind.valueOf(resultSet.getString("kind")),
                resultSet.getString("resource_source"),
                resultSet.getString("resource_external_key"),
                resultSet.getString("url"),
                resultSet.getString("memo"),
                ResourceReportStatus.valueOf(resultSet.getString("status")),
                toInstant(resultSet, "created_at"),
                toInstant(resultSet, "updated_at"),
                toInstant(resultSet, "handled_at"),
                resultSet.getObject("handled_by", Long.class)
        );
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}

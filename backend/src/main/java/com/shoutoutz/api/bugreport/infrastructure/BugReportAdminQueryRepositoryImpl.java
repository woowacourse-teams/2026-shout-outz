package com.shoutoutz.api.bugreport.infrastructure;

import com.shoutoutz.api.bugreport.application.BugReportAdminQueryRepository;
import com.shoutoutz.api.bugreport.application.dto.BugReportCursor;
import com.shoutoutz.api.bugreport.application.dto.BugReportPage;
import com.shoutoutz.api.bugreport.domain.BugReport;
import com.shoutoutz.api.bugreport.domain.BugReportStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BugReportAdminQueryRepositoryImpl implements BugReportAdminQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public BugReportPage findAll(BugReportStatus status, BugReportCursor cursor, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    id,
                    content,
                    reporter_user_id,
                    status,
                    created_at,
                    updated_at,
                    status_changed_at,
                    status_changed_by_user_id
                FROM bug_reports
                WHERE 1 = 1
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("limit", size + 1);
        if (status != null) {
            sql.append("AND status = :status\n");
            parameters.addValue("status", status.name());
        }
        if (cursor != null) {
            sql.append("AND (created_at, id) < (:cursorCreatedAt, :cursorBugReportId)\n");
            parameters
                    .addValue(
                            "cursorCreatedAt",
                            OffsetDateTime.ofInstant(cursor.createdAt(), ZoneOffset.UTC)
                    )
                    .addValue("cursorBugReportId", cursor.bugReportId());
        }
        sql.append("ORDER BY created_at DESC, id DESC\n");
        sql.append("LIMIT :limit");

        List<BugReport> bugReports = jdbcTemplate.query(
                sql.toString(),
                parameters,
                (resultSet, rowNumber) -> BugReport.reconstitute(
                        resultSet.getLong("id"),
                        resultSet.getString("content"),
                        resultSet.getObject("reporter_user_id", Long.class),
                        BugReportStatus.valueOf(resultSet.getString("status")),
                        resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
                        resultSet.getObject("updated_at", OffsetDateTime.class).toInstant(),
                        resultSet.getObject("status_changed_at", OffsetDateTime.class) == null
                                ? null
                                : resultSet.getObject("status_changed_at", OffsetDateTime.class)
                                        .toInstant(),
                        resultSet.getObject("status_changed_by_user_id", Long.class)
                )
        );
        return createPage(bugReports, size, countAll(status));
    }

    private long countAll(BugReportStatus status) {
        String sql = "SELECT COUNT(*) FROM bug_reports";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (status != null) {
            sql += " WHERE status = :status";
            parameters.addValue("status", status.name());
        }
        return jdbcTemplate.queryForObject(sql, parameters, Long.class);
    }

    private BugReportPage createPage(List<BugReport> bugReports, int size, long totalCount) {
        if (bugReports.size() <= size) {
            return new BugReportPage(bugReports, false, totalCount);
        }
        return new BugReportPage(bugReports.subList(0, size), true, totalCount);
    }
}

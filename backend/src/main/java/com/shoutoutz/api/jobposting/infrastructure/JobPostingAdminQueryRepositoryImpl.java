package com.shoutoutz.api.jobposting.infrastructure;

import com.shoutoutz.api.jobposting.application.JobPostingAdminQueryRepository;
import com.shoutoutz.api.jobposting.application.dto.JobPostingCursor;
import com.shoutoutz.api.jobposting.application.dto.JobPostingPage;
import com.shoutoutz.api.jobposting.application.dto.JobPostingSummary;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.MatchingStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JobPostingAdminQueryRepositoryImpl implements JobPostingAdminQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public JobPostingPage findAll(JobPostingCursor cursor, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    id,
                    company_name,
                    title,
                    job_role,
                    status,
                    hidden_at,
                    created_at
                FROM job_postings
                WHERE 1 = 1
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("limit", size + 1);
        if (cursor != null) {
            sql.append("AND id < :cursorJobPostingId\n");
            parameters.addValue("cursorJobPostingId", cursor.jobPostingId());
        }
        sql.append("ORDER BY id DESC\n");
        sql.append("LIMIT :limit");

        List<JobPostingSummary> summaries = jdbcTemplate.query(
                sql.toString(),
                parameters,
                (resultSet, rowNumber) -> new JobPostingSummary(
                        resultSet.getLong("id"),
                        resultSet.getString("company_name"),
                        resultSet.getString("title"),
                        JobRole.valueOf(resultSet.getString("job_role")),
                        MatchingStatus.valueOf(resultSet.getString("status")),
                        toInstant(resultSet.getObject("hidden_at", OffsetDateTime.class)),
                        resultSet.getObject("created_at", OffsetDateTime.class).toInstant()
                )
        );
        return createPage(summaries, size, countAll());
    }

    private Instant toInstant(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant();
    }

    private long countAll() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_postings", new MapSqlParameterSource(), Long.class);
    }

    private JobPostingPage createPage(List<JobPostingSummary> summaries, int size, long totalCount) {
        if (summaries.size() <= size) {
            return new JobPostingPage(summaries, false, totalCount);
        }
        return new JobPostingPage(summaries.subList(0, size), true, totalCount);
    }
}

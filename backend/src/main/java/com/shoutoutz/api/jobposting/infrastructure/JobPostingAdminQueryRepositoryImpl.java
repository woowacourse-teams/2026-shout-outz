package com.shoutoutz.api.jobposting.infrastructure;

import com.shoutoutz.api.jobposting.application.JobPostingAdminQueryRepository;
import com.shoutoutz.api.jobposting.application.dto.CandidateChunk;
import com.shoutoutz.api.jobposting.application.dto.JobPostingCursor;
import com.shoutoutz.api.jobposting.application.dto.JobPostingPage;
import com.shoutoutz.api.jobposting.application.dto.JobPostingSummary;
import com.shoutoutz.api.jobposting.application.dto.RequirementRecommendationDetail;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.MatchingFailure;
import com.shoutoutz.api.jobposting.domain.MatchingStatus;
import com.shoutoutz.api.jobposting.domain.RecommendationVerdict;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class JobPostingAdminQueryRepositoryImpl implements JobPostingAdminQueryRepository {

    private static final TypeReference<List<CandidateChunk>> EVIDENCE_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public JobPostingPage findAll(JobPostingCursor cursor, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    id,
                    company_name,
                    title,
                    job_role,
                    status,
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
                        resultSet.getObject("created_at", OffsetDateTime.class).toInstant()
                )
        );
        return createPage(summaries, size, countAll());
    }

    @Override
    public List<RequirementRecommendationDetail> findRecommendations(long jobPostingId) {
        return jdbcTemplate.query("""
                SELECT
                    recommendation.job_requirement_id,
                    recommendation.resource_source,
                    recommendation.resource_external_key,
                    document.title,
                    document.url,
                    recommendation.verdict,
                    recommendation.reason,
                    recommendation.description,
                    recommendation.similarity_score,
                    recommendation.evidence
                FROM requirement_recommendations recommendation
                JOIN job_requirements requirement ON requirement.id = recommendation.job_requirement_id
                LEFT JOIN resource_documents document
                    ON document.source = recommendation.resource_source
                    AND document.external_key = recommendation.resource_external_key
                    AND document.deleted_at IS NULL
                WHERE requirement.job_posting_id = :jobPostingId
                ORDER BY
                    recommendation.job_requirement_id,
                    CASE recommendation.verdict WHEN 'MATCH' THEN 0 WHEN 'PARTIAL' THEN 1 ELSE 2 END,
                    recommendation.similarity_score DESC,
                    recommendation.id
                """,
                new MapSqlParameterSource("jobPostingId", jobPostingId),
                (resultSet, rowNumber) -> new RequirementRecommendationDetail(
                        resultSet.getLong("job_requirement_id"),
                        resultSet.getString("resource_source"),
                        resultSet.getString("resource_external_key"),
                        resultSet.getString("title"),
                        resultSet.getString("url"),
                        RecommendationVerdict.valueOf(resultSet.getString("verdict")),
                        resultSet.getString("reason"),
                        resultSet.getString("description"),
                        resultSet.getDouble("similarity_score"),
                        objectMapper.readValue(resultSet.getString("evidence"), EVIDENCE_TYPE)
                )
        );
    }

    @Override
    public Map<Long, MatchingFailure> findLastErrors(long jobPostingId) {
        return jdbcTemplate.query("""
                SELECT id, last_error
                FROM job_requirements
                WHERE job_posting_id = :jobPostingId AND last_error IS NOT NULL
                """,
                new MapSqlParameterSource("jobPostingId", jobPostingId),
                (resultSet, rowNumber) -> Map.entry(
                        resultSet.getLong("id"),
                        MatchingFailure.valueOf(resultSet.getString("last_error"))
                )
        ).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

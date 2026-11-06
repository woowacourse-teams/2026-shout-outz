package com.shoutoutz.api.jobposting.infrastructure;

import com.shoutoutz.api.jobposting.application.JobPostingQueryRepository;
import com.shoutoutz.api.jobposting.application.dto.JobPostingListItem;
import com.shoutoutz.api.jobposting.application.dto.JobPostingListPage;
import com.shoutoutz.api.jobposting.application.dto.JobPostingSearchCondition;
import com.shoutoutz.api.jobposting.domain.JobRole;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JobPostingQueryRepositoryImpl implements JobPostingQueryRepository {

    private static final int REQUIREMENT_PREVIEW_SIZE = 3;

    /**
     * 사용자에게 보여 줄 공고. 마감일 당일까지 보인다.
     */
    private static final String VISIBLE_CONDITION = """
            p.status = 'READY'
              AND p.hidden_at IS NULL
              AND (p.deadline IS NULL OR p.deadline >= :today)
            """;

    /**
     * 회사 이름, 공고 제목, 역량 이름 중 하나라도 검색어를 포함하면 남긴다.
     */
    private static final String KEYWORD_CONDITION = """
              AND (
                  p.company_name ILIKE :keywordPattern ESCAPE '\\'
                  OR p.title ILIKE :keywordPattern ESCAPE '\\'
                  OR EXISTS (
                      SELECT 1
                      FROM job_requirements r
                      WHERE r.job_posting_id = p.id
                        AND r.name ILIKE :keywordPattern ESCAPE '\\'
                  )
              )
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public JobPostingListPage findAll(JobPostingSearchCondition condition) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("today", condition.today())
                .addValue("limit", condition.size() + 1);
        StringBuilder sql = new StringBuilder("""
                SELECT
                    p.id,
                    p.company_name,
                    p.job_role,
                    p.employment_type,
                    p.career_requirement,
                    p.title,
                    p.deadline,
                    p.created_at
                FROM job_postings p
                WHERE
                """).append(VISIBLE_CONDITION);
        if (condition.jobRole() != null) {
            sql.append("  AND p.job_role = :jobRole\n");
            parameters.addValue("jobRole", condition.jobRole().name());
        }
        appendKeywordCondition(sql, parameters, condition.keyword());
        if (condition.cursor() != null) {
            sql.append("  AND (p.created_at, p.id) < (:cursorCreatedAt, :cursorJobPostingId)\n");
            parameters.addValue("cursorCreatedAt",
                    OffsetDateTime.ofInstant(condition.cursor().createdAt(), ZoneOffset.UTC));
            parameters.addValue("cursorJobPostingId", condition.cursor().jobPostingId());
        }
        sql.append("ORDER BY p.created_at DESC, p.id DESC\nLIMIT :limit");

        List<PostingRow> fetched = jdbcTemplate.query(sql.toString(), parameters, this::mapPostingRow);

        // 다음 페이지가 있는지 알기 위해 한 개를 더 조회하고, 응답에서는 뺀다.
        boolean hasNext = fetched.size() > condition.size();
        List<PostingRow> rows = hasNext ? fetched.subList(0, condition.size()) : fetched;
        return new JobPostingListPage(withRequirements(rows), hasNext);
    }

    @Override
    public Map<JobRole, Long> countByJobRole(String keyword, LocalDate today) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("today", today);
        StringBuilder sql = new StringBuilder("""
                SELECT p.job_role, COUNT(*) AS posting_count
                FROM job_postings p
                WHERE
                """).append(VISIBLE_CONDITION);
        appendKeywordCondition(sql, parameters, keyword);
        sql.append("GROUP BY p.job_role");

        Map<JobRole, Long> counts = new EnumMap<>(JobRole.class);
        jdbcTemplate.query(sql.toString(), parameters, resultSet -> {
            counts.put(JobRole.valueOf(resultSet.getString("job_role")), resultSet.getLong("posting_count"));
        });
        return counts;
    }

    @Override
    public Map<Long, Long> countRecommendationsByRequirement(long jobPostingId) {
        Map<Long, Long> counts = new HashMap<>();
        jdbcTemplate.query("""
                        SELECT recommendation.job_requirement_id, COUNT(*) AS recommendation_count
                        FROM requirement_recommendations recommendation
                        JOIN job_requirements requirement ON requirement.id = recommendation.job_requirement_id
                        JOIN resource_documents document
                            ON document.source = recommendation.resource_source
                            AND document.external_key = recommendation.resource_external_key
                            AND document.deleted_at IS NULL
                        WHERE requirement.job_posting_id = :jobPostingId
                          AND recommendation.verdict IN ('MATCH', 'PARTIAL')
                        GROUP BY recommendation.job_requirement_id
                        """,
                new MapSqlParameterSource("jobPostingId", jobPostingId),
                resultSet -> {
                    counts.put(resultSet.getLong("job_requirement_id"), resultSet.getLong("recommendation_count"));
                });
        return counts;
    }

    private void appendKeywordCondition(StringBuilder sql, MapSqlParameterSource parameters, String keyword) {
        if (keyword == null) {
            return;
        }
        sql.append(KEYWORD_CONDITION);
        parameters.addValue("keywordPattern", "%" + escapeLikePattern(keyword) + "%");
    }

    /**
     * 페이지에 담긴 공고의 역량을 한 번에 읽어, 공고마다 역량 순서대로 앞 3개 이름과 전체 개수를 붙인다.
     */
    private List<JobPostingListItem> withRequirements(List<PostingRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream().map(PostingRow::id).toList();
        Map<Long, List<String>> namesByPostingId = jdbcTemplate.query("""
                        SELECT job_posting_id, name
                        FROM job_requirements
                        WHERE job_posting_id IN (:ids)
                        ORDER BY job_posting_id, display_order
                        """,
                        new MapSqlParameterSource("ids", ids),
                        (resultSet, rowNumber) -> Map.entry(resultSet.getLong("job_posting_id"), resultSet.getString("name")))
                .stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey, LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        return rows.stream()
                .map(row -> {
                    List<String> names = namesByPostingId.getOrDefault(row.id(), List.of());
                    return new JobPostingListItem(
                            row.id(),
                            row.companyName(),
                            row.jobRole(),
                            row.employmentType(),
                            row.careerRequirement(),
                            row.title(),
                            row.deadline(),
                            names.size(),
                            names.subList(0, Math.min(names.size(), REQUIREMENT_PREVIEW_SIZE)),
                            row.createdAt()
                    );
                })
                .toList();
    }

    private PostingRow mapPostingRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PostingRow(
                resultSet.getLong("id"),
                resultSet.getString("company_name"),
                JobRole.valueOf(resultSet.getString("job_role")),
                resultSet.getString("employment_type"),
                resultSet.getString("career_requirement"),
                resultSet.getString("title"),
                resultSet.getObject("deadline", LocalDate.class),
                resultSet.getObject("created_at", OffsetDateTime.class).toInstant()
        );
    }

    /**
     * 검색어의 LIKE 와일드카드(%, _)를 일반 문자로 검색하기 위한 이스케이프
     */
    private static String escapeLikePattern(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private record PostingRow(
            long id,
            String companyName,
            JobRole jobRole,
            String employmentType,
            String careerRequirement,
            String title,
            LocalDate deadline,
            Instant createdAt
    ) {
    }
}

package com.shoutoutz.api.project.infrastructure.jdbc;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.project.domain.ProjectCursor;
import com.shoutoutz.api.project.domain.ProjectFilterCondition;
import com.shoutoutz.api.project.domain.ProjectFilterOptions;
import com.shoutoutz.api.project.domain.ProjectFilterOptions.CohortCount;
import com.shoutoutz.api.project.domain.ProjectFilterOptions.TechTagCount;
import com.shoutoutz.api.project.domain.ProjectMemberProfile;
import com.shoutoutz.api.project.domain.ProjectPage;
import com.shoutoutz.api.project.domain.ProjectSearchCondition;
import com.shoutoutz.api.project.domain.ProjectSort;
import com.shoutoutz.api.project.domain.ProjectSummary;
import com.shoutoutz.api.project.domain.ProjectTechTag;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 프로젝트 목록 카드의 기본 정보와 필터 선택지별 프로젝트 수를 조회하는 JDBC 조회 클래스
 * 검색어와 필터가 있을 때만 조건을 붙이는 동적 쿼리이며, 목록, 전체 개수, 필터 선택지별 프로젝트 수는 같은 조건으로 조회한다.
 */
@Repository
@RequiredArgsConstructor
public class ProjectListJdbcRepository {

    /**
     * 공개된 프로젝트와 카드에 필요한 좋아요 수, 댓글 수
     * 댓글 수는 삭제된 댓글을 제외하고 대댓글을 포함한다.
     */
    private static final String PUBLIC_PROJECTS_SQL = """
            SELECT
                p.id,
                p.slug,
                p.title,
                p.tagline,
                p.cohort,
                p.thumbnail_media_id,
                p.registered_by,
                p.created_at,
                (
                    SELECT COUNT(*)
                    FROM project_reactions r
                    WHERE r.project_id = p.id
                      AND r.reaction_type = 'LIKE'
                ) AS like_count,
                (
                    SELECT COUNT(*)
                    FROM project_comments c
                    WHERE c.project_id = p.id
                      AND c.deleted_at IS NULL
                ) AS comment_count
            FROM projects p
            WHERE p.approval_status = 'APPROVED'
              AND p.deleted_at IS NULL
            """;

    private static final String COHORTS_CONDITION = """
              AND p.cohort IN (:cohorts)
            """;

    /**
     * 선택한 기술 스택을 모두 사용한 프로젝트만 남긴다(AND).
     * 선택한 기술 스택 중 프로젝트가 가진 개수가 선택한 개수와 같아야 한다.
     */
    private static final String TECH_TAGS_CONDITION = """
              AND (
                  SELECT COUNT(DISTINCT pt.tech_tag_id)
                  FROM project_tags pt
                  WHERE pt.project_id = p.id
                    AND pt.tech_tag_id IN (:techTagIds)
              ) = :techTagCount
            """;

    /**
     * 프로젝트 이름, 한 줄 소개, 기술 스택 이름, 참여 크루 이름 중 하나라도 검색어를 포함하면 남긴다.
     * 크루 이름은 상세 조회에서 보이는 이름과 맞춘다.
     * - 탈퇴한 팀원은 이름을 숨기므로 검색되지 않는다.
     * - 가입하지 않은 이관 팀원은 GitHub 이름과 GitHub 아이디로, 가입해서 매칭된 이관 팀원은 프로필 이름으로 검색된다.
     */
    private static final String KEYWORD_CONDITION = """
              AND (
                  p.title ILIKE :keywordPattern ESCAPE '\\'
                  OR p.tagline ILIKE :keywordPattern ESCAPE '\\'
                  OR EXISTS (
                      SELECT 1
                      FROM project_tags pt
                      JOIN tech_tags t ON t.id = pt.tech_tag_id
                      WHERE pt.project_id = p.id
                        AND t.display_name ILIKE :keywordPattern ESCAPE '\\'
                  )
                  OR EXISTS (
                      SELECT 1
                      FROM project_members pm
                      JOIN users u ON u.id = pm.user_id
                      JOIN user_profiles up ON up.user_id = u.id
                      WHERE pm.project_id = p.id
                        AND u.deleted_at IS NULL
                        AND up.display_name ILIKE :keywordPattern ESCAPE '\\'
                  )
                  OR EXISTS (
                      SELECT 1
                      FROM woowa_archived_project_members am
                      LEFT JOIN users u ON u.id = am.matched_user_id
                      LEFT JOIN user_profiles up ON up.user_id = u.id
                      WHERE am.project_id = p.id
                        AND (
                            (
                                am.matched_user_id IS NULL
                                AND (
                                    am.display_name ILIKE :keywordPattern ESCAPE '\\'
                                    OR am.github_login ILIKE :keywordPattern ESCAPE '\\'
                                )
                            )
                            OR (
                                am.matched_user_id IS NOT NULL
                                AND u.deleted_at IS NULL
                                AND up.display_name ILIKE :keywordPattern ESCAPE '\\'
                            )
                        )
                  )
              )
            """;

    private static final String COHORT_COUNTS_SQL = """
            SELECT cohort, COUNT(*) AS project_count
            FROM filtered
            GROUP BY cohort
            """;

    /**
     * 활성 기술 스택마다, 조건에 맞는 프로젝트 중 그 기술 스택을 사용한 프로젝트 수
     * 기술 스택 필터는 AND라서, 이 수가 곧 그 기술 스택을 추가로 선택했을 때의 프로젝트 수다.
     * 사용한 프로젝트가 없는 기술 스택도 0으로 포함하며, 기술 스택 선택지 조회와 같은 이름순으로 정렬한다.
     */
    private static final String TECH_TAG_COUNTS_SQL = """
            SELECT t.id, t.display_name, COALESCE(c.project_count, 0) AS project_count
            FROM tech_tags t
            LEFT JOIN (
                SELECT pt.tech_tag_id, COUNT(*) AS project_count
                FROM project_tags pt
                JOIN filtered f ON f.id = pt.project_id
                GROUP BY pt.tech_tag_id
            ) c ON c.tech_tag_id = t.id
            WHERE t.is_active = true
            ORDER BY LOWER(t.display_name), t.id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ProjectTechTagAndMemberJdbcRepository techTagAndMemberJdbcRepository;

    public ProjectPage findAll(ProjectSearchCondition condition) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        String filteredProjectsSql = filteredProjectsSql(condition.filter(), parameters);

        List<ProjectSummary> fetched = jdbcTemplate.query(
                pageSql(filteredProjectsSql, condition, parameters),
                parameters,
                ProjectListJdbcRepository::mapSummary
        );
        Long totalCount = jdbcTemplate.queryForObject(
                "WITH filtered AS (" + filteredProjectsSql + ") SELECT COUNT(*) FROM filtered",
                parameters,
                Long.class
        );

        // 다음 페이지가 있는지 알기 위해 한 개를 더 조회하고, 응답에서는 뺀다.
        boolean hasNext = fetched.size() > condition.size();
        List<ProjectSummary> items = hasNext ? fetched.subList(0, condition.size()) : fetched;
        return new ProjectPage(withTechTagsAndMembers(items), hasNext, totalCount == null ? 0 : totalCount);
    }

    /**
     * 필터 모달의 기수, 기술 스택 선택지와 선택지별 프로젝트 수를 조회한다.
     * 기수는 여러 개를 고를 수 있어서(OR), 6기를 골라도 5기, 7기 숫자가 0이 되지 않도록 기수 선택은 빼고 센다.
     */
    public ProjectFilterOptions findFilterOptions(ProjectFilterCondition condition) {
        return new ProjectFilterOptions(
                countByCohort(condition.withoutCohorts()),
                countByTechTag(condition),
                countMatched(condition)
        );
    }

    /**
     * 프로젝트가 없는 기수도 0으로 채워, 최신 기수부터 반환한다.
     */
    private List<CohortCount> countByCohort(ProjectFilterCondition condition) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        Map<Integer, Long> counts = jdbcTemplate.query(
                        withFiltered(filteredProjectsSql(condition, parameters), COHORT_COUNTS_SQL),
                        parameters,
                        (resultSet, rowNumber) -> Map.entry(resultSet.getInt("cohort"), resultSet.getLong("project_count"))
                ).stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return Cohort.descending().stream()
                .map(cohort -> new CohortCount(cohort, counts.getOrDefault(cohort.getValue(), 0L)))
                .toList();
    }

    private List<TechTagCount> countByTechTag(ProjectFilterCondition condition) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        return jdbcTemplate.query(
                withFiltered(filteredProjectsSql(condition, parameters), TECH_TAG_COUNTS_SQL),
                parameters,
                (resultSet, rowNumber) -> new TechTagCount(
                        resultSet.getLong("id"),
                        resultSet.getString("display_name"),
                        resultSet.getLong("project_count")
                )
        );
    }

    private long countMatched(ProjectFilterCondition condition) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        Long count = jdbcTemplate.queryForObject(
                withFiltered(filteredProjectsSql(condition, parameters), "SELECT COUNT(*) FROM filtered"),
                parameters,
                Long.class
        );
        return count == null ? 0 : count;
    }

    private static String withFiltered(String filteredProjectsSql, String selectSql) {
        return "WITH filtered AS (" + filteredProjectsSql + ")\n" + selectSql;
    }

    /**
     * 한 페이지 카드의 기술 스택과 팀원을 카드 수와 상관없이 한 번에 조회해 붙인다.
     * 이관 프로젝트는 이관 팀원 테이블에서, 새로 등록된 프로젝트는 팀원 테이블에서 조회한다.
     */
    private List<ProjectSummary> withTechTagsAndMembers(List<ProjectSummary> projects) {
        if (projects.isEmpty()) {
            return projects;
        }
        Map<Long, List<ProjectTechTag>> techTags = techTagAndMemberJdbcRepository.findTechTags(
                projects.stream().map(ProjectSummary::id).toList());
        Map<Long, List<ProjectMemberProfile>> members = new HashMap<>();
        members.putAll(techTagAndMemberJdbcRepository.findMembers(projects.stream()
                .filter(project -> !project.isArchived())
                .map(ProjectSummary::id)
                .toList()));
        members.putAll(techTagAndMemberJdbcRepository.findArchivedMembers(projects.stream()
                .filter(ProjectSummary::isArchived)
                .collect(Collectors.toMap(ProjectSummary::id, ProjectSummary::cohort))));

        return projects.stream()
                .map(project -> project.withTechTagsAndMembers(
                        techTags.getOrDefault(project.id(), List.of()),
                        members.getOrDefault(project.id(), List.of())
                ))
                .toList();
    }

    /**
     * 공개 범위, 검색어, 필터를 적용한 프로젝트
     * 목록과 전체 개수가 같은 조건을 쓴다.
     */
    private static String filteredProjectsSql(ProjectFilterCondition condition, MapSqlParameterSource parameters) {
        StringBuilder sql = new StringBuilder(PUBLIC_PROJECTS_SQL);
        if (!condition.cohorts().isEmpty()) {
            sql.append(COHORTS_CONDITION);
            parameters.addValue("cohorts", condition.cohorts());
        }
        if (!condition.techTagIds().isEmpty()) {
            sql.append(TECH_TAGS_CONDITION);
            parameters.addValue("techTagIds", condition.techTagIds());
            parameters.addValue("techTagCount", condition.techTagIds().size());
        }
        if (condition.keyword() != null) {
            sql.append(KEYWORD_CONDITION);
            parameters.addValue("keywordPattern", "%" + escapeLikePattern(condition.keyword()) + "%");
        }
        return sql.toString();
    }

    /**
     * 커서가 있으면 커서 위치 다음부터, 정렬 기준대로 조회 개수보다 하나 더 조회한다.
     * 정렬의 마지막 기준을 항상 id로 두어, 등록 시각이나 좋아요 수가 같아도 순서가 하나로 정해진다.
     * 커서 조건은 정렬 기준과 같은 순서로 (값1, 값2, ...) < (커서 값1, 커서 값2, ...) 를 비교한다.
     */
    private static String pageSql(
            String filteredProjectsSql,
            ProjectSearchCondition condition,
            MapSqlParameterSource parameters
    ) {
        boolean popular = condition.sort() == ProjectSort.POPULAR;
        StringBuilder sql = new StringBuilder("WITH filtered AS (")
                .append(filteredProjectsSql)
                .append(")\nSELECT * FROM filtered\n");

        ProjectCursor cursor = condition.cursor();
        if (cursor != null) {
            sql.append(popular
                    ? "WHERE (like_count, created_at, id) < (:cursorLikeCount, :cursorCreatedAt, :cursorId)\n"
                    : "WHERE (created_at, id) < (:cursorCreatedAt, :cursorId)\n");
            parameters.addValue("cursorLikeCount", cursor.likeCount());
            parameters.addValue("cursorCreatedAt", OffsetDateTime.ofInstant(cursor.createdAt(), ZoneOffset.UTC));
            parameters.addValue("cursorId", cursor.id());
        }

        sql.append(popular
                ? "ORDER BY like_count DESC, created_at DESC, id DESC\n"
                : "ORDER BY created_at DESC, id DESC\n");
        sql.append("LIMIT :limit");
        parameters.addValue("limit", condition.size() + 1);
        return sql.toString();
    }

    private static ProjectSummary mapSummary(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ProjectSummary(
                resultSet.getLong("id"),
                resultSet.getString("slug"),
                resultSet.getString("title"),
                resultSet.getString("tagline"),
                resultSet.getInt("cohort"),
                resultSet.getObject("thumbnail_media_id", Long.class),
                resultSet.getObject("registered_by", Long.class),
                resultSet.getLong("like_count"),
                resultSet.getLong("comment_count"),
                List.of(),
                List.of(),
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
}

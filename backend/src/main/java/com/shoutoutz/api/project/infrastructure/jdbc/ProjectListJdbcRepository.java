package com.shoutoutz.api.project.infrastructure.jdbc;

import com.shoutoutz.api.project.domain.ProjectCursor;
import com.shoutoutz.api.project.domain.ProjectPage;
import com.shoutoutz.api.project.domain.ProjectSearchCondition;
import com.shoutoutz.api.project.domain.ProjectSort;
import com.shoutoutz.api.project.domain.ProjectSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 프로젝트 목록 카드의 기본 정보를 조회하는 JDBC 조회 클래스
 * 검색어와 필터가 있을 때만 조건을 붙이는 동적 쿼리이며, 목록과 전체 개수는 같은 조건으로 조회한다.
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

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ProjectPage findAll(ProjectSearchCondition condition) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        String filteredProjectsSql = filteredProjectsSql(condition, parameters);

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
        return new ProjectPage(items, hasNext, totalCount == null ? 0 : totalCount);
    }

    /**
     * 공개 범위, 검색어, 필터를 적용한 프로젝트
     * 목록과 전체 개수가 같은 조건을 쓴다.
     */
    private static String filteredProjectsSql(ProjectSearchCondition condition, MapSqlParameterSource parameters) {
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

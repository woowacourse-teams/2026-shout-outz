package com.shoutoutz.api.project.infrastructure.jdbc;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.ProjectDetail;
import com.shoutoutz.api.project.domain.ServiceStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 프로젝트 상세 화면에 필요한 여러 테이블을 조합하는 JDBC 조회 클래스
 */
@Repository
@RequiredArgsConstructor
public class ProjectDetailJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Optional<ProjectDetail> findDetailById(long projectId, Long viewerId) {
        return findProject(projectId, viewerId)
                .map(project -> project.withTechTagsAndMembers(
                        findTechTags(projectId),
                        project.isArchived()
                                ? findArchivedMembers(projectId, project.cohort())
                                : findMembers(projectId)
                ));
    }

    /**
     * 프로젝트 기본 정보와 반려 사유, 리액션 및 댓글 수를 조회한다.
     *
     * 반려 사유는 REJECTED 상태일 때만 가장 최근 반려 이력의 사유를 쓴다.
     * 댓글 수는 삭제된 댓글을 제외하고, 대댓글을 포함한다.
     * 비로그인이면 viewerId가 null이라, user_id 비교가 거짓이 되어, likedByMe와 bookmarkedByMe는 false다.
     */
    private Optional<ProjectDetail> findProject(long projectId, Long viewerId) {
        String sql = """
                SELECT
                    p.id,
                    p.slug,
                    p.title,
                    p.team_name,
                    p.tagline,
                    p.cohort,
                    p.thumbnail_media_id,
                    p.description_md,
                    p.github_repository_url,
                    p.deployment_url,
                    p.service_status,
                    p.approval_status,
                    p.registered_by,
                    p.view_count,
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
                    ) AS comment_count,
                    EXISTS (
                        SELECT 1
                        FROM project_reactions r
                        WHERE r.project_id = p.id
                          AND r.user_id = :viewerId
                          AND r.reaction_type = 'LIKE'
                    ) AS liked_by_me,
                    EXISTS (
                        SELECT 1
                        FROM project_reactions r
                        WHERE r.project_id = p.id
                          AND r.user_id = :viewerId
                          AND r.reaction_type = 'BOOKMARK'
                    ) AS bookmarked_by_me
                FROM projects p
                WHERE p.id = :projectId
                  AND p.deleted_at IS NULL
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("viewerId", viewerId, Types.BIGINT);

        return jdbcTemplate.query(sql, parameters, (resultSet, rowNumber) -> new ProjectDetail(
                        resultSet.getLong("id"),
                        resultSet.getString("slug"),
                        resultSet.getString("title"),
                        resultSet.getString("team_name"),
                        resultSet.getString("tagline"),
                        resultSet.getInt("cohort"),
                        resultSet.getObject("thumbnail_media_id", Long.class),
                        resultSet.getString("description_md"),
                        resultSet.getString("github_repository_url"),
                        resultSet.getString("deployment_url"),
                        ServiceStatus.valueOf(resultSet.getString("service_status")),
                        ApprovalStatus.valueOf(resultSet.getString("approval_status")),
                        resultSet.getString("reject_reason"),
                        resultSet.getObject("registered_by", Long.class),
                        resultSet.getInt("view_count"),
                        resultSet.getObject("star_count", Integer.class),
                        resultSet.getLong("like_count"),
                        resultSet.getLong("bookmark_count"),
                        resultSet.getBoolean("liked_by_me"),
                        resultSet.getBoolean("bookmarked_by_me"),
                        resultSet.getLong("comment_count"),
                        List.of(),
                        List.of(),
                        resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
                        resultSet.getObject("updated_at", OffsetDateTime.class).toInstant()
                ))
                .stream()
                .findFirst();
    }

    private List<ProjectDetail.TechTag> findTechTags(long projectId) {
        String sql = """
                SELECT t.id, t.display_name
                FROM project_tags pt
                JOIN tech_tags t ON t.id = pt.tech_tag_id
                WHERE pt.project_id = :projectId
                ORDER BY pt.display_order, t.id
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("projectId", projectId),
                (resultSet, rowNumber) -> new ProjectDetail.TechTag(
                        resultSet.getLong("id"),
                        resultSet.getString("display_name")
                )
        );
    }

    /**
     * 새로 등록된 프로젝트의 팀원을 등록 순서대로 조회한다. 등록 시 등록자가 첫 번째로 저장된다.
     * 탈퇴 30일이 지나 프로필이 삭제된 팀원도 목록에서 빠지지 않도록, 프로필은 LEFT JOIN 한다.
     */
    private List<ProjectDetail.Member> findMembers(long projectId) {
        String sql = """
                SELECT
                    u.id AS user_id,
                    u.handle,
                    u.deleted_at,
                    COALESCE(up.display_name, u.handle) AS display_name,
                    up.cohort,
                    up.track,
                    up.avatar_image_id
                FROM project_members pm
                JOIN users u ON u.id = pm.user_id
                LEFT JOIN user_profiles up ON up.user_id = u.id
                WHERE pm.project_id = :projectId
                ORDER BY pm.display_order, pm.user_id
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("projectId", projectId),
                (resultSet, rowNumber) -> mapUserMember(resultSet)
        );
    }

    /**
     * 이관 프로젝트의 팀원을 조회한다.
     * 가입해서 매칭된 팀원은 실제 사용자 프로필로, 매칭되지 않은 팀원은 GitHub 정보로 보여준다.
     * GitHub 이름이 없으면 GitHub 아이디로 채운다.
     */
    private List<ProjectDetail.Member> findArchivedMembers(long projectId, int projectCohort) {
        String sql = """
                SELECT
                    COALESCE(NULLIF(btrim(am.display_name), ''), am.github_login) AS archived_display_name,
                    am.avatar_url,
                    am.github_profile_url,
                    u.id AS user_id,
                    u.handle,
                    u.deleted_at,
                    COALESCE(up.display_name, u.handle) AS display_name,
                    up.cohort,
                    up.track,
                    up.avatar_image_id
                FROM woowa_archived_project_members am
                LEFT JOIN users u ON u.id = am.matched_user_id
                LEFT JOIN user_profiles up ON up.user_id = u.id
                WHERE am.project_id = :projectId
                ORDER BY am.display_order, am.id
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("projectId", projectId),
                (resultSet, rowNumber) -> {
                    if (resultSet.getObject("user_id") == null) {
                        return ProjectDetail.Member.archived(
                                resultSet.getString("archived_display_name"),
                                projectCohort,
                                resultSet.getString("avatar_url"),
                                resultSet.getString("github_profile_url")
                        );
                    }
                    return mapUserMember(resultSet);
                }
        );
    }

    /**
     * 탈퇴 여부는 users.deleted_at 으로 판정한다. 30일 유예 중이라 프로필이 남아 있어도 탈퇴한 사용자로 보여준다.
     */
    private ProjectDetail.Member mapUserMember(ResultSet resultSet) throws SQLException {
        long userId = resultSet.getLong("user_id");
        String handle = resultSet.getString("handle");
        if (resultSet.getObject("deleted_at") != null) {
            return ProjectDetail.Member.withdrawn(userId, handle);
        }
        return ProjectDetail.Member.user(
                userId,
                handle,
                resultSet.getString("display_name"),
                resultSet.getObject("cohort", Integer.class),
                resultSet.getString("track"),
                resultSet.getObject("avatar_image_id", Long.class)
        );
    }
}

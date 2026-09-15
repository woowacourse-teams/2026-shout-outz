package com.shoutoutz.api.project.infrastructure.jdbc;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.ProjectDetail;
import com.shoutoutz.api.project.domain.ProjectMemberProfile;
import com.shoutoutz.api.project.domain.ServiceStatus;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 프로젝트 상세 화면에 필요한 여러 테이블을 조합하는 JDBC 조회 클래스
 * 기술 스택과 팀원은 목록 조회와 같은 규칙으로 보여주도록 공통 조회를 쓴다.
 */
@Repository
@RequiredArgsConstructor
public class ProjectDetailJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ProjectTechTagAndMemberJdbcRepository techTagAndMemberJdbcRepository;

    public Optional<ProjectDetail> findDetailById(long projectId, Long viewerId) {
        return findProject(projectId, viewerId)
                .map(project -> project.withTechTagsAndMembers(
                        techTagAndMemberJdbcRepository.findTechTags(List.of(projectId))
                                .getOrDefault(projectId, List.of()),
                        findMembers(project).getOrDefault(projectId, List.of())
                ));
    }

    private Map<Long, List<ProjectMemberProfile>> findMembers(ProjectDetail project) {
        if (project.isArchived()) {
            return techTagAndMemberJdbcRepository.findArchivedMembers(Map.of(project.id(), project.cohort()));
        }
        return techTagAndMemberJdbcRepository.findMembers(List.of(project.id()));
    }

    /**
     * 프로젝트 기본 정보와 반려 사유, 리액션 및 댓글 수를 조회한다.
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
}

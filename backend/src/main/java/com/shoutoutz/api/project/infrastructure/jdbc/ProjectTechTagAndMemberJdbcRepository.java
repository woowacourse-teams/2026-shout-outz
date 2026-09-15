package com.shoutoutz.api.project.infrastructure.jdbc;

import com.shoutoutz.api.project.domain.ProjectMemberProfile;
import com.shoutoutz.api.project.domain.ProjectTechTag;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 프로젝트의 기술 스택과 팀원을 조회하는 JDBC 조회 클래스
 * 상세 조회와 목록 조회가 같은 규칙으로 보여주도록 함께 쓴다.
 * 여러 프로젝트를 한 번에 조회해, 목록에서 카드마다 쿼리가 나가지 않게 한다. (N+1 방지)
 * 결과는 프로젝트 ID별로 묶으며, 각 목록은 등록 순서(display_order)를 따른다.
 */
@Repository
@RequiredArgsConstructor
public class ProjectTechTagAndMemberJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Map<Long, List<ProjectTechTag>> findTechTags(Collection<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        String sql = """
                SELECT pt.project_id, t.id, t.display_name
                FROM project_tags pt
                JOIN tech_tags t ON t.id = pt.tech_tag_id
                WHERE pt.project_id IN (:projectIds)
                ORDER BY pt.project_id, pt.display_order, t.id
                """;

        Map<Long, List<ProjectTechTag>> techTags = new HashMap<>();
        jdbcTemplate.query(sql, new MapSqlParameterSource("projectIds", projectIds), resultSet -> {
            techTags.computeIfAbsent(resultSet.getLong("project_id"), projectId -> new ArrayList<>())
                    .add(new ProjectTechTag(resultSet.getLong("id"), resultSet.getString("display_name")));
        });
        return techTags;
    }

    /**
     * 새로 등록된 프로젝트의 팀원을 등록 순서대로 조회한다. 등록 시 등록자가 첫 번째로 저장된다.
     * 탈퇴 30일이 지나 프로필이 삭제된 팀원도 목록에서 빠지지 않도록, 프로필은 LEFT JOIN 한다.
     */
    public Map<Long, List<ProjectMemberProfile>> findMembers(Collection<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        String sql = """
                SELECT
                    pm.project_id,
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
                WHERE pm.project_id IN (:projectIds)
                ORDER BY pm.project_id, pm.display_order, pm.user_id
                """;

        Map<Long, List<ProjectMemberProfile>> members = new HashMap<>();
        jdbcTemplate.query(sql, new MapSqlParameterSource("projectIds", projectIds), resultSet -> {
            members.computeIfAbsent(resultSet.getLong("project_id"), projectId -> new ArrayList<>())
                    .add(mapUserMember(resultSet));
        });
        return members;
    }

    /**
     * 이관 프로젝트의 팀원을 조회한다.
     * 가입해서 매칭된 팀원은 실제 사용자 프로필로, 매칭되지 않은 팀원은 GitHub 정보로 보여준다.
     * GitHub 이름이 없으면 GitHub 아이디로 채우고, 기수는 프로젝트 기수로 채운다.
     *
     * @param projectCohorts 이관 프로젝트 ID별 프로젝트 기수
     */
    public Map<Long, List<ProjectMemberProfile>> findArchivedMembers(Map<Long, Integer> projectCohorts) {
        if (projectCohorts.isEmpty()) {
            return Map.of();
        }
        String sql = """
                SELECT
                    am.project_id,
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
                WHERE am.project_id IN (:projectIds)
                ORDER BY am.project_id, am.display_order, am.id
                """;

        Map<Long, List<ProjectMemberProfile>> members = new HashMap<>();
        jdbcTemplate.query(sql, new MapSqlParameterSource("projectIds", projectCohorts.keySet()), resultSet -> {
            long projectId = resultSet.getLong("project_id");
            ProjectMemberProfile member = resultSet.getObject("user_id") == null
                    ? ProjectMemberProfile.archived(
                    resultSet.getString("archived_display_name"),
                    projectCohorts.get(projectId),
                    resultSet.getString("avatar_url"),
                    resultSet.getString("github_profile_url")
            )
                    : mapUserMember(resultSet);
            members.computeIfAbsent(projectId, id -> new ArrayList<>()).add(member);
        });
        return members;
    }

    /**
     * 탈퇴 여부는 users.deleted_at 으로 판정한다. 30일 유예 중이라 프로필이 남아 있어도 탈퇴한 사용자로 보여준다.
     */
    private static ProjectMemberProfile mapUserMember(ResultSet resultSet) throws SQLException {
        long userId = resultSet.getLong("user_id");
        String handle = resultSet.getString("handle");
        if (resultSet.getObject("deleted_at") != null) {
            return ProjectMemberProfile.withdrawn(userId, handle);
        }
        return ProjectMemberProfile.user(
                userId,
                handle,
                resultSet.getString("display_name"),
                resultSet.getObject("cohort", Integer.class),
                resultSet.getString("track"),
                resultSet.getObject("avatar_image_id", Long.class)
        );
    }
}

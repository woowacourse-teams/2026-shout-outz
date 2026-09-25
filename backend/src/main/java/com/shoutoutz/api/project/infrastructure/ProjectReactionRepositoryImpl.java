package com.shoutoutz.api.project.infrastructure;

import com.shoutoutz.api.project.domain.ProjectReactionCounts;
import com.shoutoutz.api.project.domain.ProjectReactionRepository;
import com.shoutoutz.api.project.domain.ProjectReactionType;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectReactionRepositoryImpl implements ProjectReactionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void add(long projectId, long userId, ProjectReactionType type) {
        jdbcTemplate.update(
                """
                        INSERT INTO project_reactions (project_id, user_id, reaction_type)
                        VALUES (:projectId, :userId, :reactionType)
                        ON CONFLICT (project_id, user_id, reaction_type) DO NOTHING
                        """,
                new MapSqlParameterSource()
                        .addValue("projectId", projectId)
                        .addValue("userId", userId)
                        .addValue("reactionType", type.name())
        );
    }

    @Override
    public void remove(long projectId, long userId, ProjectReactionType type) {
        jdbcTemplate.update(
                """
                        DELETE FROM project_reactions
                        WHERE project_id = :projectId
                          AND user_id = :userId
                          AND reaction_type = :reactionType
                        """,
                new MapSqlParameterSource()
                        .addValue("projectId", projectId)
                        .addValue("userId", userId)
                        .addValue("reactionType", type.name())
        );
    }

    @Override
    public ProjectReactionCounts countByProjectId(long projectId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FILTER (WHERE reaction_type = 'LIKE') AS like_count,
                               COUNT(*) FILTER (WHERE reaction_type = 'BOOKMARK') AS bookmark_count
                        FROM project_reactions
                        WHERE project_id = :projectId
                        """,
                Map.of("projectId", projectId),
                (resultSet, rowNumber) -> new ProjectReactionCounts(
                        resultSet.getLong("like_count"),
                        resultSet.getLong("bookmark_count")
                )
        );
    }
}

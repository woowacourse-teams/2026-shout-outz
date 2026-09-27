package com.shoutoutz.api.comment.infrastructure;

import com.shoutoutz.api.comment.domain.ProjectCommentReactionCounts;
import com.shoutoutz.api.comment.domain.ProjectCommentReactionRepository;
import com.shoutoutz.api.comment.domain.ProjectCommentReactionType;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectCommentReactionRepositoryImpl implements ProjectCommentReactionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void add(long commentId, long userId, ProjectCommentReactionType type) {
        jdbcTemplate.update(
                """
                        INSERT INTO project_comment_reactions (comment_id, user_id, reaction_type)
                        VALUES (:commentId, :userId, :reactionType)
                        ON CONFLICT (comment_id, user_id, reaction_type) DO NOTHING
                        """,
                new MapSqlParameterSource()
                        .addValue("commentId", commentId)
                        .addValue("userId", userId)
                        .addValue("reactionType", type.name())
        );
    }

    @Override
    public boolean remove(long commentId, long userId, ProjectCommentReactionType type) {
        return jdbcTemplate.update(
                """
                        DELETE FROM project_comment_reactions
                        WHERE comment_id = :commentId
                          AND user_id = :userId
                          AND reaction_type = :reactionType
                        """,
                new MapSqlParameterSource()
                        .addValue("commentId", commentId)
                        .addValue("userId", userId)
                        .addValue("reactionType", type.name())
        ) == 1;
    }

    @Override
    public ProjectCommentReactionCounts countByCommentId(long commentId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FILTER (WHERE reaction_type = 'AGREE') AS agree_count
                        FROM project_comment_reactions
                        WHERE comment_id = :commentId
                        """,
                Map.of("commentId", commentId),
                (resultSet, rowNumber) -> new ProjectCommentReactionCounts(
                        resultSet.getLong("agree_count")
                )
        );
    }
}

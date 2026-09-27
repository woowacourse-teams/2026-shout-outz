package com.shoutoutz.api.comment.infrastructure;

import com.shoutoutz.api.comment.domain.FeedCommentReactionCounts;
import com.shoutoutz.api.comment.domain.FeedCommentReactionRepository;
import com.shoutoutz.api.comment.domain.FeedCommentReactionType;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedCommentReactionRepositoryImpl implements FeedCommentReactionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void add(long commentId, long userId, FeedCommentReactionType type) {
        jdbcTemplate.update(
                """
                        INSERT INTO feed_comment_reactions (comment_id, user_id, reaction_type)
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
    public void remove(long commentId, long userId, FeedCommentReactionType type) {
        jdbcTemplate.update(
                """
                        DELETE FROM feed_comment_reactions
                        WHERE comment_id = :commentId
                          AND user_id = :userId
                          AND reaction_type = :reactionType
                        """,
                new MapSqlParameterSource()
                        .addValue("commentId", commentId)
                        .addValue("userId", userId)
                        .addValue("reactionType", type.name())
        );
    }

    @Override
    public FeedCommentReactionCounts countByCommentId(long commentId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FILTER (WHERE reaction_type = 'AGREE') AS agree_count
                        FROM feed_comment_reactions
                        WHERE comment_id = :commentId
                        """,
                Map.of("commentId", commentId),
                (resultSet, rowNumber) -> new FeedCommentReactionCounts(
                        resultSet.getLong("agree_count")
                )
        );
    }
}

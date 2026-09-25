package com.shoutoutz.api.feed.infrastructure;

import com.shoutoutz.api.feed.domain.FeedReactionCounts;
import com.shoutoutz.api.feed.domain.FeedReactionRepository;
import com.shoutoutz.api.feed.domain.FeedReactionType;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedReactionRepositoryImpl implements FeedReactionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void add(long feedId, long userId, FeedReactionType type) {
        jdbcTemplate.update(
                """
                        INSERT INTO feed_reactions (feed_id, user_id, reaction_type)
                        VALUES (:feedId, :userId, :reactionType)
                        ON CONFLICT (feed_id, user_id, reaction_type) DO NOTHING
                        """,
                new MapSqlParameterSource()
                        .addValue("feedId", feedId)
                        .addValue("userId", userId)
                        .addValue("reactionType", type.name())
        );
    }

    @Override
    public void remove(long feedId, long userId, FeedReactionType type) {
        jdbcTemplate.update(
                """
                        DELETE FROM feed_reactions
                        WHERE feed_id = :feedId
                          AND user_id = :userId
                          AND reaction_type = :reactionType
                        """,
                new MapSqlParameterSource()
                        .addValue("feedId", feedId)
                        .addValue("userId", userId)
                        .addValue("reactionType", type.name())
        );
    }

    @Override
    public FeedReactionCounts countByFeedId(long feedId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FILTER (WHERE reaction_type = 'LIKE') AS like_count,
                               COUNT(*) FILTER (WHERE reaction_type = 'BOOKMARK') AS bookmark_count
                        FROM feed_reactions
                        WHERE feed_id = :feedId
                        """,
                Map.of("feedId", feedId),
                (resultSet, rowNumber) -> new FeedReactionCounts(
                        resultSet.getLong("like_count"),
                        resultSet.getLong("bookmark_count")
                )
        );
    }
}

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

    /**
     * 반응이 실제로 저장됐을 때만 좋아요 수를 1 올린다. 이미 있는 반응이면 저장도 증가도 하지 않는다.
     */
    @Override
    public void add(long feedId, long userId, FeedReactionType type) {
        jdbcTemplate.update(
                """
                        WITH inserted AS (
                            INSERT INTO feed_reactions (feed_id, user_id, reaction_type)
                            VALUES (:feedId, :userId, :reactionType)
                            ON CONFLICT (feed_id, user_id, reaction_type) DO NOTHING
                            RETURNING feed_id, reaction_type
                        )
                        UPDATE feeds
                        SET like_count = like_count + 1
                        WHERE id IN (SELECT feed_id FROM inserted WHERE reaction_type = 'LIKE')
                        """,
                new MapSqlParameterSource()
                        .addValue("feedId", feedId)
                        .addValue("userId", userId)
                        .addValue("reactionType", type.name())
        );
    }

    /**
     * 반응이 실제로 지워졌을 때만 좋아요 수를 1 내린다.
     */
    @Override
    public boolean remove(long feedId, long userId, FeedReactionType type) {
        Long deletedCount = jdbcTemplate.queryForObject(
                """
                        WITH deleted AS (
                            DELETE FROM feed_reactions
                            WHERE feed_id = :feedId
                              AND user_id = :userId
                              AND reaction_type = :reactionType
                            RETURNING feed_id, reaction_type
                        ),
                        decreased AS (
                            UPDATE feeds
                            SET like_count = like_count - 1
                            WHERE id IN (SELECT feed_id FROM deleted WHERE reaction_type = 'LIKE')
                        )
                        SELECT COUNT(*) FROM deleted
                        """,
                new MapSqlParameterSource()
                        .addValue("feedId", feedId)
                        .addValue("userId", userId)
                        .addValue("reactionType", type.name()),
                Long.class
        );
        return deletedCount != null && deletedCount == 1;
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

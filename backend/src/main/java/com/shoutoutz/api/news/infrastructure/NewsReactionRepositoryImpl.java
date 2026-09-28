package com.shoutoutz.api.news.infrastructure;

import com.shoutoutz.api.news.domain.NewsReactionRepository;
import com.shoutoutz.api.news.domain.NewsReactionType;
import com.shoutoutz.api.news.domain.NewsReactionCounts;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NewsReactionRepositoryImpl implements NewsReactionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void add(long newsId, long userId, NewsReactionType type) {
        jdbcTemplate.update(
                """
                        INSERT INTO news_reactions (news_id, user_id, reaction_type)
                        VALUES (:newsId, :userId, :reactionType)
                        ON CONFLICT (news_id, user_id, reaction_type) DO NOTHING
                        """,
                new MapSqlParameterSource()
                        .addValue("newsId", newsId)
                        .addValue("userId", userId)
                        .addValue("reactionType", type.name())
        );
    }

    @Override
    public boolean remove(long newsId, long userId, NewsReactionType type) {
        return jdbcTemplate.update(
                """
                        DELETE FROM news_reactions
                        WHERE news_id = :newsId
                          AND user_id = :userId
                          AND reaction_type = :reactionType
                        """,
                new MapSqlParameterSource()
                        .addValue("newsId", newsId)
                        .addValue("userId", userId)
                        .addValue("reactionType", type.name())
        ) == 1;
    }

    @Override
    public long countByNewsId(long newsId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM news_reactions
                        WHERE news_id = :newsId
                          AND reaction_type = 'LIKE'
                        """,
                Map.of("newsId", newsId),
                Long.class
        );
    }

    @Override
    public NewsReactionCounts findCountsByNewsId(long newsId, Long viewerId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FILTER (WHERE reaction_type = 'LIKE') AS like_count,
                               COALESCE(
                                       BOOL_OR(
                                               user_id = CAST(:viewerId AS BIGINT)
                                                   AND reaction_type = 'LIKE'
                                       ),
                                       false
                               ) AS liked_by_me
                        FROM news_reactions
                        WHERE news_id = :newsId
                        """,
                new MapSqlParameterSource()
                        .addValue("newsId", newsId)
                        .addValue("viewerId", viewerId, Types.BIGINT),
                (resultSet, rowNumber) -> new NewsReactionCounts(
                        resultSet.getLong("like_count"),
                        resultSet.getBoolean("liked_by_me")
                )
        );
    }

    @Override
    public Map<Long, NewsReactionCounts> findCountsByNewsIds(
            List<Long> newsIds,
            Long viewerId
    ) {
        if (newsIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, NewsReactionCounts> counts = new HashMap<>();
        jdbcTemplate.query(
                """
                        SELECT news_id,
                               COUNT(*) FILTER (WHERE reaction_type = 'LIKE') AS like_count,
                               COALESCE(
                                       BOOL_OR(
                                               user_id = CAST(:viewerId AS BIGINT)
                                                   AND reaction_type = 'LIKE'
                                       ),
                                       false
                               ) AS liked_by_me
                        FROM news_reactions
                        WHERE news_id IN (:newsIds)
                        GROUP BY news_id
                        """,
                new MapSqlParameterSource()
                        .addValue("newsIds", newsIds)
                        .addValue("viewerId", viewerId, Types.BIGINT),
                (RowCallbackHandler) resultSet -> counts.put(
                        resultSet.getLong("news_id"),
                        new NewsReactionCounts(
                                resultSet.getLong("like_count"),
                                resultSet.getBoolean("liked_by_me")
                        )
                )
        );
        return counts;
    }
}

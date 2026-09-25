package com.shoutoutz.api.news.infrastructure;

import com.shoutoutz.api.news.domain.NewsReactionRepository;
import com.shoutoutz.api.news.domain.NewsReactionType;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
    public void remove(long newsId, long userId, NewsReactionType type) {
        jdbcTemplate.update(
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
        );
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
}

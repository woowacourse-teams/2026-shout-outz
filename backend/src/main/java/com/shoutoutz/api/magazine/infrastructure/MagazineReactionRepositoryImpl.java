package com.shoutoutz.api.magazine.infrastructure;

import com.shoutoutz.api.magazine.domain.MagazineReactionRepository;
import com.shoutoutz.api.magazine.domain.MagazineReactionType;
import com.shoutoutz.api.magazine.domain.MagazineReactionCounts;
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
public class MagazineReactionRepositoryImpl implements MagazineReactionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void add(long magazineId, long userId, MagazineReactionType type) {
        jdbcTemplate.update(
                """
                        INSERT INTO magazine_reactions (magazine_id, user_id, reaction_type)
                        VALUES (:magazineId, :userId, :reactionType)
                        ON CONFLICT (magazine_id, user_id, reaction_type) DO NOTHING
                        """,
                new MapSqlParameterSource()
                        .addValue("magazineId", magazineId)
                        .addValue("userId", userId)
                        .addValue("reactionType", type.name())
        );
    }

    @Override
    public boolean remove(long magazineId, long userId, MagazineReactionType type) {
        return jdbcTemplate.update(
                """
                        DELETE FROM magazine_reactions
                        WHERE magazine_id = :magazineId
                          AND user_id = :userId
                          AND reaction_type = :reactionType
                        """,
                new MapSqlParameterSource()
                        .addValue("magazineId", magazineId)
                        .addValue("userId", userId)
                        .addValue("reactionType", type.name())
        ) == 1;
    }

    @Override
    public long countByMagazineId(long magazineId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM magazine_reactions
                        WHERE magazine_id = :magazineId
                          AND reaction_type = 'LIKE'
                        """,
                Map.of("magazineId", magazineId),
                Long.class
        );
    }

    @Override
    public MagazineReactionCounts findCountsByMagazineId(long magazineId, Long viewerId) {
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
                        FROM magazine_reactions
                        WHERE magazine_id = :magazineId
                        """,
                new MapSqlParameterSource()
                        .addValue("magazineId", magazineId)
                        .addValue("viewerId", viewerId, Types.BIGINT),
                (resultSet, rowNumber) -> new MagazineReactionCounts(
                        resultSet.getLong("like_count"),
                        resultSet.getBoolean("liked_by_me")
                )
        );
    }

    @Override
    public Map<Long, MagazineReactionCounts> findCountsByMagazineIds(
            List<Long> magazineIds,
            Long viewerId
    ) {
        if (magazineIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, MagazineReactionCounts> counts = new HashMap<>();
        jdbcTemplate.query(
                """
                        SELECT magazine_id,
                               COUNT(*) FILTER (WHERE reaction_type = 'LIKE') AS like_count,
                               COALESCE(
                                       BOOL_OR(
                                               user_id = CAST(:viewerId AS BIGINT)
                                                   AND reaction_type = 'LIKE'
                                       ),
                                       false
                               ) AS liked_by_me
                        FROM magazine_reactions
                        WHERE magazine_id IN (:magazineIds)
                        GROUP BY magazine_id
                        """,
                new MapSqlParameterSource()
                        .addValue("magazineIds", magazineIds)
                        .addValue("viewerId", viewerId, Types.BIGINT),
                (RowCallbackHandler) resultSet -> counts.put(
                        resultSet.getLong("magazine_id"),
                        new MagazineReactionCounts(
                                resultSet.getLong("like_count"),
                                resultSet.getBoolean("liked_by_me")
                        )
                )
        );
        return counts;
    }
}

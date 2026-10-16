package com.shoutoutz.api.interview.infrastructure;

import com.shoutoutz.api.interview.domain.InterviewReactionRepository;
import com.shoutoutz.api.interview.domain.InterviewReactionType;
import com.shoutoutz.api.interview.domain.InterviewReactionCounts;
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
public class InterviewReactionRepositoryImpl implements InterviewReactionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void add(long interviewId, long userId, InterviewReactionType type) {
        jdbcTemplate.update(
                """
                        INSERT INTO interview_reactions (interview_id, user_id, reaction_type)
                        VALUES (:interviewId, :userId, :reactionType)
                        ON CONFLICT (interview_id, user_id, reaction_type) DO NOTHING
                        """,
                new MapSqlParameterSource()
                        .addValue("interviewId", interviewId)
                        .addValue("userId", userId)
                        .addValue("reactionType", type.name())
        );
    }

    @Override
    public boolean remove(long interviewId, long userId, InterviewReactionType type) {
        return jdbcTemplate.update(
                """
                        DELETE FROM interview_reactions
                        WHERE interview_id = :interviewId
                          AND user_id = :userId
                          AND reaction_type = :reactionType
                        """,
                new MapSqlParameterSource()
                        .addValue("interviewId", interviewId)
                        .addValue("userId", userId)
                        .addValue("reactionType", type.name())
        ) == 1;
    }

    @Override
    public long countByInterviewId(long interviewId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM interview_reactions
                        WHERE interview_id = :interviewId
                          AND reaction_type = 'LIKE'
                        """,
                Map.of("interviewId", interviewId),
                Long.class
        );
    }

    @Override
    public InterviewReactionCounts findCountsByInterviewId(long interviewId, Long viewerId) {
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
                        FROM interview_reactions
                        WHERE interview_id = :interviewId
                        """,
                new MapSqlParameterSource()
                        .addValue("interviewId", interviewId)
                        .addValue("viewerId", viewerId, Types.BIGINT),
                (resultSet, rowNumber) -> new InterviewReactionCounts(
                        resultSet.getLong("like_count"),
                        resultSet.getBoolean("liked_by_me")
                )
        );
    }

    @Override
    public Map<Long, InterviewReactionCounts> findCountsByInterviewIds(
            List<Long> interviewIds,
            Long viewerId
    ) {
        if (interviewIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, InterviewReactionCounts> counts = new HashMap<>();
        jdbcTemplate.query(
                """
                        SELECT interview_id,
                               COUNT(*) FILTER (WHERE reaction_type = 'LIKE') AS like_count,
                               COALESCE(
                                       BOOL_OR(
                                               user_id = CAST(:viewerId AS BIGINT)
                                                   AND reaction_type = 'LIKE'
                                       ),
                                       false
                               ) AS liked_by_me
                        FROM interview_reactions
                        WHERE interview_id IN (:interviewIds)
                        GROUP BY interview_id
                        """,
                new MapSqlParameterSource()
                        .addValue("interviewIds", interviewIds)
                        .addValue("viewerId", viewerId, Types.BIGINT),
                (RowCallbackHandler) resultSet -> counts.put(
                        resultSet.getLong("interview_id"),
                        new InterviewReactionCounts(
                                resultSet.getLong("like_count"),
                                resultSet.getBoolean("liked_by_me")
                        )
                )
        );
        return counts;
    }
}

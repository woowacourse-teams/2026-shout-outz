package com.shoutoutz.api.user.infrastructure;

import com.shoutoutz.api.user.application.query.UserProfileCounts;
import com.shoutoutz.api.user.application.query.UserProfileCountsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserProfileCountsRepositoryImpl implements UserProfileCountsRepository {

    private static final String COUNT_SQL = """
            SELECT
                (
                    SELECT COUNT(*)
                    FROM project_members pm
                    JOIN projects p ON p.id = pm.project_id
                    WHERE pm.user_id = ?
                      AND p.deleted_at IS NULL
                ) AS projects,
                (
                    SELECT COUNT(*)
                    FROM posts p
                    WHERE p.author_id = ?
                      AND p.deleted_at IS NULL
                ) AS posts
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public UserProfileCounts countByUserId(long userId) {
        return jdbcTemplate.queryForObject(
                COUNT_SQL,
                (resultSet, rowNumber) -> new UserProfileCounts(
                        resultSet.getLong("projects"),
                        resultSet.getLong("posts")
                ),
                userId,
                userId
        );
    }
}

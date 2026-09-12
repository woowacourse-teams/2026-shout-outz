package com.shoutoutz.api.user.infrastructure;

import com.shoutoutz.api.user.application.UserQueryRepository;
import com.shoutoutz.api.user.application.dto.UserProfileCounts;
import com.shoutoutz.api.user.application.dto.UserSearchCursor;
import com.shoutoutz.api.user.application.dto.UserSearchItem;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserQueryRepositoryImpl implements UserQueryRepository {

    private static final String COUNT_SQL = """
            SELECT
                (
                    SELECT COUNT(*)
                    FROM project_members pm
                    JOIN projects p ON p.id = pm.project_id
                    WHERE pm.user_id = :userId
                      AND p.deleted_at IS NULL
                ) AS projects,
                (
                    SELECT COUNT(*)
                    FROM posts p
                    WHERE p.author_id = :userId
                      AND p.deleted_at IS NULL
                ) AS posts
            """;
    private static final String RANKED_WOOWA_USERS_SQL = """
            WITH ranked_woowa_users AS (
                SELECT
                    u.handle,
                    up.display_name,
                    up.user_type,
                    up.track,
                    up.cohort,
                    up.avatar_image_id,
                    CASE
                        WHEN lower(u.handle) = lower(:keyword)
                          OR lower(up.display_name) = lower(:keyword) THEN 0
                        WHEN lower(u.handle) LIKE lower(:prefixPattern) ESCAPE '\\'
                          OR lower(up.display_name) LIKE lower(:prefixPattern) ESCAPE '\\' THEN 1
                        ELSE 2
                    END AS relevance_rank
                FROM users u
                JOIN user_profiles up ON up.user_id = u.id
                WHERE u.status = 'ACTIVE'
                  AND up.user_type IN ('WOOWACOURSE_CREW', 'WOOWACOURSE_COACH')
                  AND (
                      lower(u.handle) LIKE lower(:containsPattern) ESCAPE '\\'
                      OR lower(up.display_name) LIKE lower(:containsPattern) ESCAPE '\\'
                  )
            )
            """;
    private static final String FIRST_SEARCH_SQL = RANKED_WOOWA_USERS_SQL + """
            SELECT *
            FROM ranked_woowa_users
            ORDER BY relevance_rank, lower(display_name), lower(handle)
            LIMIT :limit
            """;
    private static final String NEXT_SEARCH_SQL = RANKED_WOOWA_USERS_SQL + """
            SELECT *
            FROM ranked_woowa_users
            WHERE relevance_rank > :cursorRelevanceRank
               OR (
                    relevance_rank = :cursorRelevanceRank
                    AND lower(display_name) > lower(:cursorDisplayName)
               )
               OR (
                    relevance_rank = :cursorRelevanceRank
                    AND lower(display_name) = lower(:cursorDisplayName)
                    AND lower(handle) > lower(:cursorHandle)
               )
            ORDER BY relevance_rank, lower(display_name), lower(handle)
            LIMIT :limit
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public UserProfileCounts countByUserId(long userId) {
        return jdbcTemplate.queryForObject(
                COUNT_SQL,
                new MapSqlParameterSource("userId", userId),
                (resultSet, rowNumber) -> new UserProfileCounts(
                        resultSet.getLong("projects"),
                        resultSet.getLong("posts")
                )
        );
    }

    @Override
    public List<UserSearchItem> searchWoowaUsers(
            String keyword,
            UserSearchCursor cursor,
            int limit
    ) {
        String escapedKeyword = escapeLikePattern(keyword);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("keyword", keyword)
                .addValue("prefixPattern", escapedKeyword + "%")
                .addValue("containsPattern", "%" + escapedKeyword + "%")
                .addValue("limit", limit);
        String sql = FIRST_SEARCH_SQL;
        if (cursor != null) {
            sql = NEXT_SEARCH_SQL;
            parameters
                    .addValue("cursorRelevanceRank", cursor.relevanceRank())
                    .addValue("cursorDisplayName", cursor.displayName())
                    .addValue("cursorHandle", cursor.handle());
        }

        return jdbcTemplate.query(
                sql,
                parameters,
                (resultSet, rowNumber) -> new UserSearchItem(
                        resultSet.getString("handle"),
                        resultSet.getString("display_name"),
                        UserType.valueOf(resultSet.getString("user_type")),
                        resultSet.getString("track"),
                        resultSet.getObject("cohort", Short.class),
                        resultSet.getObject("avatar_image_id", Long.class),
                        resultSet.getInt("relevance_rank")
                )
        );
    }

    private String escapeLikePattern(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}

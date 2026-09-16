package com.shoutoutz.api.user.infrastructure;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.user.application.UserQueryRepository;
import com.shoutoutz.api.user.application.dto.UserProfileCounts;
import com.shoutoutz.api.user.application.dto.UserSearchCursor;
import com.shoutoutz.api.user.application.dto.UserSearchItem;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.user.domain.profile.Track;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 여러 User 관련 테이블을 조합하는 JDBC 조회 구현체.
 */
@Repository
@RequiredArgsConstructor
public class UserQueryRepositoryImpl implements UserQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public UserProfileCounts countByUserId(long userId) {
        String sql = """
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
                        FROM feeds p
                        WHERE p.author_id = :userId
                          AND p.deleted_at IS NULL
                    ) AS feeds
                """;

        return jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("userId", userId),
                (resultSet, rowNumber) -> new UserProfileCounts(
                        resultSet.getLong("projects"),
                        resultSet.getLong("feeds")
                )
        );
    }

    @Override
    public List<UserSearchItem> searchWoowaMember(
            String keyword,
            UserSearchCursor cursor,
            int limit
    ) {
        String escapedKeyword = escapeLikePattern(keyword);
        String sql = """
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
                SELECT *
                FROM ranked_woowa_users
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("keyword", keyword)
                .addValue("prefixPattern", escapedKeyword + "%")
                .addValue("containsPattern", "%" + escapedKeyword + "%")
                .addValue("limit", limit);

        if (cursor != null) {
            sql += """
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
                    """;
            parameters
                    .addValue("cursorRelevanceRank", cursor.relevanceRank())
                    .addValue("cursorDisplayName", cursor.displayName())
                    .addValue("cursorHandle", cursor.handle());
        }
        sql += """
                ORDER BY relevance_rank, lower(display_name), lower(handle)
                LIMIT :limit
                """;

        return jdbcTemplate.query(
                sql,
                parameters,
                (resultSet, rowNumber) -> new UserSearchItem(
                        resultSet.getString("handle"),
                        resultSet.getString("display_name"),
                        UserType.valueOf(resultSet.getString("user_type")),
                        toTrack(resultSet.getString("track")),
                        toCohort(resultSet.getObject("cohort", Short.class)),
                        resultSet.getObject("avatar_image_id", Long.class),
                        resultSet.getInt("relevance_rank")
                )
        );
    }

    private Track toTrack(String value) {
        if (value == null) {
            return null;
        }
        return Track.from(value);
    }

    private Cohort toCohort(Short value) {
        if (value == null) {
            return null;
        }
        return Cohort.from(value);
    }

    /**
     * LIKE 와일드카드를 일반 문자로 검색하기 위한 입력값 이스케이프.
     */
    private String escapeLikePattern(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}

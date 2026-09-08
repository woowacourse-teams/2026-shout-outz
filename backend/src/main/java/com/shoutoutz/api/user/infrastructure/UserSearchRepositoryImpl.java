package com.shoutoutz.api.user.infrastructure;

import com.shoutoutz.api.user.application.query.UserSearchCursor;
import com.shoutoutz.api.user.application.query.UserSearchItem;
import com.shoutoutz.api.user.application.query.UserSearchRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserSearchRepositoryImpl implements UserSearchRepository {

    private static final String RANKED_CREW_SQL = """
            WITH ranked_crew AS (
                SELECT
                    u.handle,
                    up.display_name,
                    up.user_type,
                    up.track,
                    up.cohort,
                    up.avatar_image_id,
                    CASE
                        WHEN lower(u.handle) = lower(?)
                          OR lower(up.display_name) = lower(?) THEN 0
                        WHEN lower(u.handle) LIKE lower(?) ESCAPE '\\'
                          OR lower(up.display_name) LIKE lower(?) ESCAPE '\\' THEN 1
                        ELSE 2
                    END AS relevance_rank
                FROM users u
                JOIN user_profiles up ON up.user_id = u.id
                WHERE u.status = 'ACTIVE'
                  AND up.user_type = 'WOOWACOURSE_CREW'
                  AND (
                      lower(u.handle) LIKE lower(?) ESCAPE '\\'
                      OR lower(up.display_name) LIKE lower(?) ESCAPE '\\'
                  )
            )
            """;
    private static final String FIRST_SLICE_SQL = RANKED_CREW_SQL + """
            SELECT *
            FROM ranked_crew
            ORDER BY relevance_rank, lower(display_name), lower(handle)
            LIMIT ?
            """;
    private static final String NEXT_SLICE_SQL = RANKED_CREW_SQL + """
            SELECT *
            FROM ranked_crew
            WHERE relevance_rank > ?
               OR (relevance_rank = ? AND lower(display_name) > lower(?))
               OR (
                    relevance_rank = ?
                    AND lower(display_name) = lower(?)
                    AND lower(handle) > lower(?)
               )
            ORDER BY relevance_rank, lower(display_name), lower(handle)
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<UserSearchItem> searchCrew(
            String keyword,
            UserSearchCursor cursor,
            int limit
    ) {
        String escapedKeyword = escapeLikePattern(keyword);
        String prefixPattern = escapedKeyword + "%";
        String containsPattern = "%" + escapedKeyword + "%";
        List<Object> parameters = new ArrayList<>(List.of(
                keyword,
                keyword,
                prefixPattern,
                prefixPattern,
                containsPattern,
                containsPattern
        ));
        String sql = FIRST_SLICE_SQL;
        if (cursor != null) {
            sql = NEXT_SLICE_SQL;
            parameters.add(cursor.relevanceRank());
            parameters.add(cursor.relevanceRank());
            parameters.add(cursor.displayName());
            parameters.add(cursor.relevanceRank());
            parameters.add(cursor.displayName());
            parameters.add(cursor.handle());
        }
        parameters.add(limit);

        return jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> new UserSearchItem(
                        resultSet.getString("handle"),
                        resultSet.getString("display_name"),
                        UserType.valueOf(resultSet.getString("user_type")),
                        resultSet.getString("track"),
                        resultSet.getObject("cohort", Short.class),
                        resultSet.getObject("avatar_image_id", Long.class),
                        resultSet.getInt("relevance_rank")
                ),
                parameters.toArray()
        );
    }

    private String escapeLikePattern(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}

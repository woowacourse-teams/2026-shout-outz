package com.shoutoutz.api.feed.infrastructure;

import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.feed.application.FeedQueryRepository;
import com.shoutoutz.api.feed.application.dto.FeedCursor;
import com.shoutoutz.api.feed.application.dto.FeedItem;
import com.shoutoutz.api.feed.application.dto.FeedMediaReference;
import com.shoutoutz.api.feed.application.dto.FeedSort;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.user.domain.profile.Track;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedQueryRepositoryImpl implements FeedQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Optional<FeedItem> findById(long feedId) {
        List<FeedBaseRow> rows = jdbcTemplate.query(
                """
                        SELECT p.id,
                               p.title,
                               p.content,
                               0 AS like_count,
                               0 AS comment_count,
                               0 AS relevance_rank,
                               p.created_at,
                               p.updated_at,
                               u.handle,
                               up.display_name,
                               up.user_type,
                               up.track,
                               up.cohort,
                               up.avatar_image_id
                        FROM feeds p
                        JOIN users u ON u.id = p.author_id
                        JOIN user_profiles up ON up.user_id = u.id
                        WHERE p.id = :feedId
                          AND p.deleted_at IS NULL
                        """,
                Map.of("feedId", feedId),
                (resultSet, rowNumber) -> toBaseRow(resultSet)
        );
        return assembleItems(rows).stream().findFirst();
    }

    @Override
    public List<FeedItem> findAll(
            FeedSort sort,
            Long categoryId,
            String keyword,
            FeedCursor cursor,
            int limit
    ) {
        StringBuilder sql = createFindAllQuery(sort);
        MapSqlParameterSource parameters = new MapSqlParameterSource("limit", limit);
        appendKeywordParameters(parameters, keyword);
        appendCategoryFilter(sql, parameters, categoryId);
        appendCursorAndOrder(sql, parameters, sort, cursor);
        sql.append("LIMIT :limit");

        List<FeedBaseRow> rows = jdbcTemplate.query(
                sql.toString(),
                parameters,
                (resultSet, rowNumber) -> toBaseRow(resultSet)
        );
        return assembleItems(rows);
    }

    @Override
    public List<FeedItem> findAllByAuthorId(long authorId, FeedCursor cursor, int limit) {
        StringBuilder sql = createUserFeedQuery();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("authorId", authorId)
                .addValue("limit", limit);
        sql.append("  AND p.author_id = :authorId\n");
        appendLatestCursorAndOrder(sql, parameters, cursor);
        sql.append("LIMIT :limit");

        List<FeedBaseRow> rows = jdbcTemplate.query(
                sql.toString(),
                parameters,
                (resultSet, rowNumber) -> toBaseRow(resultSet)
        );
        return assembleItems(rows);
    }

    @Override
    public List<String> findTitleSuggestions(String keyword, int limit) {
        String escapedKeyword = escapeLikePattern(keyword);
        return jdbcTemplate.queryForList(
                """
                        WITH matching_titles AS (
                            SELECT p.title,
                                   CASE
                                       WHEN lower(p.title) = lower(:keyword) THEN 0
                                       WHEN lower(p.title) LIKE lower(:prefixPattern) ESCAPE '\\' THEN 1
                                       ELSE 2
                                   END AS relevance_rank,
                                   p.created_at,
                                   p.id,
                                   row_number() OVER (
                                       PARTITION BY lower(p.title)
                                       ORDER BY p.created_at DESC, p.id DESC
                                   ) AS duplicate_rank
                            FROM feeds p
                            WHERE p.deleted_at IS NULL
                              AND lower(p.title) LIKE lower(:containsPattern) ESCAPE '\\'
                        )
                        SELECT title
                        FROM matching_titles
                        WHERE duplicate_rank = 1
                        ORDER BY relevance_rank, created_at DESC, id DESC
                        LIMIT :limit
                        """,
                new MapSqlParameterSource()
                        .addValue("keyword", keyword)
                        .addValue("prefixPattern", escapedKeyword + "%")
                        .addValue("containsPattern", "%" + escapedKeyword + "%")
                        .addValue("limit", limit),
                String.class
        );
    }

    private StringBuilder createFindAllQuery(FeedSort sort) {
        return switch (sort) {
            case LATEST -> new StringBuilder("""
                    SELECT p.id,
                           p.title,
                           p.content,
                           0 AS like_count,
                           0 AS comment_count,
                           0 AS relevance_rank,
                           p.created_at,
                           p.updated_at,
                           u.handle,
                           up.display_name,
                           up.user_type,
                           up.track,
                           up.cohort,
                           up.avatar_image_id
                    FROM feeds p
                    JOIN users u ON u.id = p.author_id
                    JOIN user_profiles up ON up.user_id = u.id
                    WHERE p.deleted_at IS NULL
                    """);
            case POPULAR -> new StringBuilder("""
                    SELECT p.id,
                           p.title,
                           p.content,
                           COALESCE(reactions.like_count, 0) AS like_count,
                           0 AS comment_count,
                           0 AS relevance_rank,
                           p.created_at,
                           p.updated_at,
                           u.handle,
                           up.display_name,
                           up.user_type,
                           up.track,
                           up.cohort,
                           up.avatar_image_id
                    FROM feeds p
                    JOIN users u ON u.id = p.author_id
                    JOIN user_profiles up ON up.user_id = u.id
                    LEFT JOIN (
                        SELECT feed_id, COUNT(*) AS like_count
                        FROM feed_reactions
                        WHERE reaction_type = 'LIKE'
                        GROUP BY feed_id
                    ) reactions ON reactions.feed_id = p.id
                    WHERE p.deleted_at IS NULL
                    """);
            case RELEVANCE -> new StringBuilder("""
                    WITH ranked_feeds AS (
                        SELECT p.id,
                               p.title,
                               p.content,
                               0 AS like_count,
                               0 AS comment_count,
                               CASE
                                   WHEN lower(p.title) = lower(:keyword) THEN 0
                                   WHEN lower(p.title) LIKE lower(:prefixPattern) ESCAPE '\\' THEN 1
                                   WHEN lower(p.title) LIKE lower(:containsPattern) ESCAPE '\\' THEN 2
                                   ELSE 3
                               END AS relevance_rank,
                               p.created_at,
                               p.updated_at,
                               u.handle,
                               up.display_name,
                               up.user_type,
                               up.track,
                               up.cohort,
                               up.avatar_image_id
                        FROM feeds p
                        JOIN users u ON u.id = p.author_id
                        JOIN user_profiles up ON up.user_id = u.id
                        WHERE p.deleted_at IS NULL
                          AND (
                              lower(p.title) LIKE lower(:containsPattern) ESCAPE '\\'
                              OR lower(p.content) LIKE lower(:containsPattern) ESCAPE '\\'
                          )
                    )
                    SELECT *
                    FROM ranked_feeds p
                    WHERE true
                    """);
        };
    }

    private StringBuilder createUserFeedQuery() {
        return new StringBuilder("""
                SELECT p.id,
                       p.title,
                       p.content,
                       (
                           SELECT COUNT(*)
                           FROM feed_reactions r
                           WHERE r.feed_id = p.id
                             AND r.reaction_type = 'LIKE'
                       ) AS like_count,
                       (
                           SELECT COUNT(*)
                           FROM feed_comments c
                           WHERE c.feed_id = p.id
                             AND c.deleted_at IS NULL
                       ) AS comment_count,
                       0 AS relevance_rank,
                       p.created_at,
                       p.updated_at,
                       u.handle,
                       up.display_name,
                       up.user_type,
                       up.track,
                       up.cohort,
                       up.avatar_image_id
                FROM feeds p
                JOIN users u ON u.id = p.author_id
                JOIN user_profiles up ON up.user_id = u.id
                WHERE p.deleted_at IS NULL
                """);
    }

    /**
     * 커서 비교 열과 정렬 열을 같은 순서로 유지해 중복과 누락 방지
     */
    private void appendCursorAndOrder(
            StringBuilder sql,
            MapSqlParameterSource parameters,
            FeedSort sort,
            FeedCursor cursor
    ) {
        switch (sort) {
            case LATEST -> appendLatestCursorAndOrder(sql, parameters, cursor);
            case POPULAR -> appendPopularCursorAndOrder(sql, parameters, cursor);
            case RELEVANCE -> appendRelevanceCursorAndOrder(sql, parameters, cursor);
        }
    }

    private void appendLatestCursorAndOrder(
            StringBuilder sql,
            MapSqlParameterSource parameters,
            FeedCursor cursor
    ) {
        if (cursor != null) {
            sql.append("""
                      AND (p.created_at, p.id) < (:cursorCreatedAt, :cursorFeedId)
                    """);
            appendCursorParameters(parameters, cursor);
        }
        sql.append("ORDER BY p.created_at DESC, p.id DESC\n");
    }

    private void appendPopularCursorAndOrder(
            StringBuilder sql,
            MapSqlParameterSource parameters,
            FeedCursor cursor
    ) {
        if (cursor != null) {
            sql.append("""
                      AND (
                          COALESCE(reactions.like_count, 0), p.created_at, p.id
                      ) < (
                          :cursorLikeCount, :cursorCreatedAt, :cursorFeedId
                      )
                    """);
            parameters.addValue("cursorLikeCount", cursor.likeCount());
            appendCursorParameters(parameters, cursor);
        }
        sql.append("""
                ORDER BY COALESCE(reactions.like_count, 0) DESC, p.created_at DESC, p.id DESC
                """);
    }

    private void appendRelevanceCursorAndOrder(
            StringBuilder sql,
            MapSqlParameterSource parameters,
            FeedCursor cursor
    ) {
        if (cursor != null) {
            sql.append("""
                      AND (
                          p.relevance_rank > :cursorRelevanceRank
                          OR (
                              p.relevance_rank = :cursorRelevanceRank
                              AND (p.created_at, p.id) < (:cursorCreatedAt, :cursorFeedId)
                          )
                      )
                    """);
            parameters.addValue("cursorRelevanceRank", cursor.relevanceRank());
            appendCursorParameters(parameters, cursor);
        }
        sql.append("""
                ORDER BY p.relevance_rank, p.created_at DESC, p.id DESC
                """);
    }

    private void appendCursorParameters(
            MapSqlParameterSource parameters,
            FeedCursor cursor
    ) {
        parameters.addValue("cursorCreatedAt", Timestamp.from(cursor.createdAt()));
        parameters.addValue("cursorFeedId", cursor.feedId());
    }

    private void appendCategoryFilter(
            StringBuilder sql,
            MapSqlParameterSource parameters,
            Long categoryId
    ) {
        if (categoryId == null) {
            return;
        }
        sql.append("""
                  AND EXISTS (
                      SELECT 1
                      FROM feed_categories filter_pc
                      JOIN categories filter_c ON filter_c.id = filter_pc.category_id
                      WHERE filter_pc.feed_id = p.id
                        AND filter_pc.category_id = :categoryId
                        AND filter_c.is_active = true
                  )
                """);
        parameters.addValue("categoryId", categoryId);
    }

    private void appendKeywordParameters(
            MapSqlParameterSource parameters,
            String keyword
    ) {
        if (keyword == null) {
            return;
        }
        String escapedKeyword = escapeLikePattern(keyword);
        parameters
                .addValue("keyword", keyword)
                .addValue("prefixPattern", escapedKeyword + "%")
                .addValue("containsPattern", "%" + escapedKeyword + "%");
    }

    private String escapeLikePattern(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    @Override
    public List<FeedMediaReference> findAllMediaByIds(List<Long> mediaIds) {
        if (mediaIds.isEmpty()) {
            return List.of();
        }

        return jdbcTemplate.query(
                """
                        SELECT id, uploaded_by, purpose, status
                        FROM media_metadata
                        WHERE id IN (:mediaIds)
                        """,
                Map.of("mediaIds", mediaIds),
                (resultSet, rowNumber) -> new FeedMediaReference(
                        resultSet.getLong("id"),
                        resultSet.getLong("uploaded_by"),
                        MediaPurpose.valueOf(resultSet.getString("purpose")),
                        MediaStatus.valueOf(resultSet.getString("status"))
                )
        );
    }

    /**
     * 조인으로 기본 행이 중복되지 않도록 카테고리와 미디어를 별도로 결합
     */
    private List<FeedItem> assembleItems(List<FeedBaseRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> feedIds = rows.stream().map(FeedBaseRow::feedId).toList();
        Map<Long, List<FeedItem.Category>> categories = loadCategories(feedIds);
        Map<Long, List<FeedItem.Media>> media = loadMedia(feedIds);
        return rows.stream()
                .map(row -> row.toItem(
                        categories.getOrDefault(row.feedId(), List.of()),
                        media.getOrDefault(row.feedId(), List.of())
                ))
                .toList();
    }

    private Map<Long, List<FeedItem.Category>> loadCategories(List<Long> feedIds) {
        return jdbcTemplate.query(
                """
                        SELECT pc.feed_id, c.id, c.slug, c.display_name, c.category_type
                        FROM feed_categories pc
                        JOIN categories c ON c.id = pc.category_id
                        WHERE pc.feed_id IN (:feedIds)
                        ORDER BY pc.feed_id, c.display_order, c.id
                        """,
                Map.of("feedIds", feedIds),
                this::extractCategories
        );
    }

    private Map<Long, List<FeedItem.Category>> extractCategories(ResultSet resultSet)
            throws SQLException {
        Map<Long, List<FeedItem.Category>> categories = new HashMap<>();
        while (resultSet.next()) {
            long feedId = resultSet.getLong("feed_id");
            FeedItem.Category category = new FeedItem.Category(
                    resultSet.getLong("id"),
                    resultSet.getString("slug"),
                    resultSet.getString("display_name"),
                    CategoryType.valueOf(resultSet.getString("category_type"))
            );
            categories.computeIfAbsent(feedId, ignored -> new ArrayList<>()).add(category);
        }
        return categories;
    }

    private Map<Long, List<FeedItem.Media>> loadMedia(List<Long> feedIds) {
        return jdbcTemplate.query(
                """
                        SELECT feed_id, media_metadata_id, display_order
                        FROM feed_media
                        WHERE feed_id IN (:feedIds)
                        ORDER BY feed_id, display_order, media_metadata_id
                        """,
                Map.of("feedIds", feedIds),
                this::extractMedia
        );
    }

    private Map<Long, List<FeedItem.Media>> extractMedia(ResultSet resultSet)
            throws SQLException {
        Map<Long, List<FeedItem.Media>> media = new HashMap<>();
        while (resultSet.next()) {
            long feedId = resultSet.getLong("feed_id");
            FeedItem.Media item = new FeedItem.Media(
                    resultSet.getLong("media_metadata_id"),
                    resultSet.getInt("display_order")
            );
            media.computeIfAbsent(feedId, ignored -> new ArrayList<>()).add(item);
        }
        return media;
    }

    private FeedBaseRow toBaseRow(ResultSet resultSet) throws SQLException {
        return new FeedBaseRow(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("content"),
                new FeedItem.Author(
                        resultSet.getString("handle"),
                        resultSet.getString("display_name"),
                        UserType.valueOf(resultSet.getString("user_type")),
                        toTrack(resultSet.getString("track")),
                        toCohort(resultSet.getObject("cohort", Short.class)),
                        resultSet.getObject("avatar_image_id", Long.class)
                ),
                resultSet.getLong("like_count"),
                resultSet.getLong("comment_count"),
                resultSet.getInt("relevance_rank"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
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

    private record FeedBaseRow(
            long feedId,
            String title,
            String content,
            FeedItem.Author author,
            long likeCount,
            long commentCount,
            int relevanceRank,
            java.time.Instant createdAt,
            java.time.Instant updatedAt
    ) {
        private FeedItem toItem(
                List<FeedItem.Category> categories,
                List<FeedItem.Media> media
        ) {
            return new FeedItem(
                    feedId,
                    title,
                    content,
                    author,
                    categories,
                    media,
                    likeCount,
                    commentCount,
                    relevanceRank,
                    createdAt,
                    updatedAt
            );
        }
    }
}

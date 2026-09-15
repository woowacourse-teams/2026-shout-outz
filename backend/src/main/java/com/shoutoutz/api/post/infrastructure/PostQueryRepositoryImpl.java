package com.shoutoutz.api.post.infrastructure;

import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.post.application.PostQueryRepository;
import com.shoutoutz.api.post.application.dto.PostCursor;
import com.shoutoutz.api.post.application.dto.PostItem;
import com.shoutoutz.api.post.application.dto.PostMediaReference;
import com.shoutoutz.api.post.application.dto.PostSort;
import com.shoutoutz.api.user.domain.profile.UserType;
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
public class PostQueryRepositoryImpl implements PostQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Optional<PostItem> findById(long postId) {
        List<PostBaseRow> rows = jdbcTemplate.query(
                """
                        SELECT p.id,
                               p.content,
                               0 AS like_count,
                               p.created_at,
                               p.updated_at,
                               u.handle,
                               up.display_name,
                               up.user_type,
                               up.track,
                               up.cohort,
                               up.avatar_image_id
                        FROM posts p
                        JOIN users u ON u.id = p.author_id
                        JOIN user_profiles up ON up.user_id = u.id
                        WHERE p.id = :postId
                          AND p.deleted_at IS NULL
                        """,
                Map.of("postId", postId),
                (resultSet, rowNumber) -> toBaseRow(resultSet)
        );
        return assembleItems(rows).stream().findFirst();
    }

    @Override
    public List<PostItem> findAll(
            PostSort sort,
            Long categoryId,
            PostCursor cursor,
            int limit
    ) {
        StringBuilder sql = createFindAllQuery(sort);
        MapSqlParameterSource parameters = new MapSqlParameterSource("limit", limit);
        appendCategoryFilter(sql, parameters, categoryId);
        appendCursorAndOrder(sql, parameters, sort, cursor);
        sql.append("LIMIT :limit");

        List<PostBaseRow> rows = jdbcTemplate.query(
                sql.toString(),
                parameters,
                (resultSet, rowNumber) -> toBaseRow(resultSet)
        );
        return assembleItems(rows);
    }

    /**
     * 최신순은 좋아요 집계를 생략하고 인기순에서만 전체 좋아요 수 집계
     */
    private StringBuilder createFindAllQuery(PostSort sort) {
        return switch (sort) {
            case LATEST -> new StringBuilder("""
                    SELECT p.id,
                           p.content,
                           0 AS like_count,
                           p.created_at,
                           p.updated_at,
                           u.handle,
                           up.display_name,
                           up.user_type,
                           up.track,
                           up.cohort,
                           up.avatar_image_id
                    FROM posts p
                    JOIN users u ON u.id = p.author_id
                    JOIN user_profiles up ON up.user_id = u.id
                    WHERE p.deleted_at IS NULL
                    """);
            case POPULAR -> new StringBuilder("""
                    SELECT p.id,
                           p.content,
                           COALESCE(reactions.like_count, 0) AS like_count,
                           p.created_at,
                           p.updated_at,
                           u.handle,
                           up.display_name,
                           up.user_type,
                           up.track,
                           up.cohort,
                           up.avatar_image_id
                    FROM posts p
                    JOIN users u ON u.id = p.author_id
                    JOIN user_profiles up ON up.user_id = u.id
                    LEFT JOIN (
                        SELECT post_id, COUNT(*) AS like_count
                        FROM post_reactions
                        WHERE reaction_type = 'LIKE'
                        GROUP BY post_id
                    ) reactions ON reactions.post_id = p.id
                    WHERE p.deleted_at IS NULL
                    """);
        };
    }

    /**
     * 커서 비교 열과 정렬 열을 같은 순서로 유지해 중복과 누락 방지
     */
    private void appendCursorAndOrder(
            StringBuilder sql,
            MapSqlParameterSource parameters,
            PostSort sort,
            PostCursor cursor
    ) {
        switch (sort) {
            case LATEST -> appendLatestCursorAndOrder(sql, parameters, cursor);
            case POPULAR -> appendPopularCursorAndOrder(sql, parameters, cursor);
        }
    }

    private void appendLatestCursorAndOrder(
            StringBuilder sql,
            MapSqlParameterSource parameters,
            PostCursor cursor
    ) {
        if (cursor != null) {
            sql.append("""
                      AND (p.created_at, p.id) < (:cursorCreatedAt, :cursorPostId)
                    """);
            appendCursorParameters(parameters, cursor);
        }
        sql.append("ORDER BY p.created_at DESC, p.id DESC\n");
    }

    private void appendPopularCursorAndOrder(
            StringBuilder sql,
            MapSqlParameterSource parameters,
            PostCursor cursor
    ) {
        if (cursor != null) {
            sql.append("""
                      AND (
                          COALESCE(reactions.like_count, 0), p.created_at, p.id
                      ) < (
                          :cursorLikeCount, :cursorCreatedAt, :cursorPostId
                      )
                    """);
            parameters.addValue("cursorLikeCount", cursor.likeCount());
            appendCursorParameters(parameters, cursor);
        }
        sql.append("""
                ORDER BY COALESCE(reactions.like_count, 0) DESC, p.created_at DESC, p.id DESC
                """);
    }

    private void appendCursorParameters(
            MapSqlParameterSource parameters,
            PostCursor cursor
    ) {
        parameters.addValue("cursorCreatedAt", Timestamp.from(cursor.createdAt()));
        parameters.addValue("cursorPostId", cursor.postId());
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
                      FROM post_categories filter_pc
                      JOIN categories filter_c ON filter_c.id = filter_pc.category_id
                      WHERE filter_pc.post_id = p.id
                        AND filter_pc.category_id = :categoryId
                        AND filter_c.is_active = true
                  )
                """);
        parameters.addValue("categoryId", categoryId);
    }

    @Override
    public List<PostMediaReference> findAllMediaByIds(List<Long> mediaIds) {
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
                (resultSet, rowNumber) -> new PostMediaReference(
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
    private List<PostItem> assembleItems(List<PostBaseRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> postIds = rows.stream().map(PostBaseRow::postId).toList();
        Map<Long, List<PostItem.Category>> categories = loadCategories(postIds);
        Map<Long, List<PostItem.Media>> media = loadMedia(postIds);
        return rows.stream()
                .map(row -> row.toItem(
                        categories.getOrDefault(row.postId(), List.of()),
                        media.getOrDefault(row.postId(), List.of())
                ))
                .toList();
    }

    private Map<Long, List<PostItem.Category>> loadCategories(List<Long> postIds) {
        return jdbcTemplate.query(
                """
                        SELECT pc.post_id, c.id, c.slug, c.display_name, c.category_type
                        FROM post_categories pc
                        JOIN categories c ON c.id = pc.category_id
                        WHERE pc.post_id IN (:postIds)
                        ORDER BY pc.post_id, c.display_order, c.id
                        """,
                Map.of("postIds", postIds),
                this::extractCategories
        );
    }

    private Map<Long, List<PostItem.Category>> extractCategories(ResultSet resultSet)
            throws SQLException {
        Map<Long, List<PostItem.Category>> categories = new HashMap<>();
        while (resultSet.next()) {
            long postId = resultSet.getLong("post_id");
            PostItem.Category category = new PostItem.Category(
                    resultSet.getLong("id"),
                    resultSet.getString("slug"),
                    resultSet.getString("display_name"),
                    CategoryType.valueOf(resultSet.getString("category_type"))
            );
            categories.computeIfAbsent(postId, ignored -> new ArrayList<>()).add(category);
        }
        return categories;
    }

    private Map<Long, List<PostItem.Media>> loadMedia(List<Long> postIds) {
        return jdbcTemplate.query(
                """
                        SELECT post_id, media_metadata_id, display_order
                        FROM post_media
                        WHERE post_id IN (:postIds)
                        ORDER BY post_id, display_order, media_metadata_id
                        """,
                Map.of("postIds", postIds),
                this::extractMedia
        );
    }

    private Map<Long, List<PostItem.Media>> extractMedia(ResultSet resultSet)
            throws SQLException {
        Map<Long, List<PostItem.Media>> media = new HashMap<>();
        while (resultSet.next()) {
            long postId = resultSet.getLong("post_id");
            PostItem.Media item = new PostItem.Media(
                    resultSet.getLong("media_metadata_id"),
                    resultSet.getInt("display_order")
            );
            media.computeIfAbsent(postId, ignored -> new ArrayList<>()).add(item);
        }
        return media;
    }

    private PostBaseRow toBaseRow(ResultSet resultSet) throws SQLException {
        return new PostBaseRow(
                resultSet.getLong("id"),
                resultSet.getString("content"),
                new PostItem.Author(
                        resultSet.getString("handle"),
                        resultSet.getString("display_name"),
                        UserType.valueOf(resultSet.getString("user_type")),
                        resultSet.getString("track"),
                        resultSet.getObject("cohort", Short.class),
                        resultSet.getObject("avatar_image_id", Long.class)
                ),
                resultSet.getLong("like_count"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }

    private record PostBaseRow(
            long postId,
            String content,
            PostItem.Author author,
            long likeCount,
            java.time.Instant createdAt,
            java.time.Instant updatedAt
    ) {
        private PostItem toItem(
                List<PostItem.Category> categories,
                List<PostItem.Media> media
        ) {
            return new PostItem(
                    postId,
                    content,
                    author,
                    categories,
                    media,
                    likeCount,
                    createdAt,
                    updatedAt
            );
        }
    }
}

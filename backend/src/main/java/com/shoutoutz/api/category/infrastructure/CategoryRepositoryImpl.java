package com.shoutoutz.api.category.infrastructure;

import com.shoutoutz.api.category.domain.Category;
import com.shoutoutz.api.category.domain.CategoryErrorCode;
import com.shoutoutz.api.category.domain.CategoryRepository;
import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.feed.domain.FeedType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Category save(Category category) {
        try {
            Category savedCategory = insertCategory(category);
            saveFeedType(savedCategory.getId(), category.getFeedType());
            return savedCategory;
        } catch (DataIntegrityViolationException exception) {
            throw translateException(exception);
        }
    }

    @Override
    public Category update(Category category) {
        try {
            return updateCategory(category);
        } catch (DataIntegrityViolationException exception) {
            throw translateException(exception);
        }
    }

    private Category insertCategory(Category category) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO categories (
                            slug, display_name, category_type, display_order, is_active
                        ) VALUES (
                            :slug, :displayName, :type, :displayOrder, :active
                        )
                        RETURNING id, slug, display_name, category_type, display_order, is_active
                        """,
                categoryParameters(category),
                (resultSet, rowNumber) -> toCategory(resultSet, category.getFeedType(), false)
        );
    }

    private Category updateCategory(Category category) {
        return jdbcTemplate.queryForObject(
                """
                        UPDATE categories
                        SET display_name = :displayName,
                            display_order = :displayOrder,
                            is_active = :active
                        WHERE id = :categoryId
                        RETURNING id, slug, display_name, category_type, display_order, is_active
                        """,
                categoryParameters(category).addValue("categoryId", category.getId()),
                (resultSet, rowNumber) -> toCategory(resultSet, category.getFeedType(), false)
        );
    }

    private MapSqlParameterSource categoryParameters(Category category) {
        return new MapSqlParameterSource()
                .addValue("slug", category.getSlug())
                .addValue("displayName", category.getDisplayName())
                .addValue("type", category.getType().name())
                .addValue("displayOrder", category.getDisplayOrder())
                .addValue("active", category.isActive());
    }

    @Override
    public Optional<Category> findById(long categoryId) {
        List<Category> categories = jdbcTemplate.query(
                """
                        SELECT c.id, c.slug, c.display_name, c.category_type,
                               c.display_order, c.is_active, cft.feed_type
                        FROM categories c
                        LEFT JOIN category_feed_types cft ON cft.category_id = c.id
                        WHERE c.id = :categoryId
                        ORDER BY cft.feed_type
                        LIMIT 1
                        """,
                Map.of("categoryId", categoryId),
                (resultSet, rowNumber) -> toCategory(resultSet, FeedType.POST, true)
        );
        return categories.stream().findFirst();
    }

    @Override
    public List<Category> findAllActive() {
        return jdbcTemplate.query(
                findAllActiveSql(null),
                new MapSqlParameterSource(),
                (resultSet, rowNumber) -> toCategory(resultSet, FeedType.POST, true)
        );
    }

    @Override
    public List<Category> findAllActiveByFeedType(FeedType feedType) {
        return jdbcTemplate.query(
                findAllActiveSql(feedType),
                categoryParameters(feedType),
                (resultSet, rowNumber) -> toCategory(resultSet, feedType, true)
        );
    }

    @Override
    public List<Category> findAllActiveByIds(List<Long> categoryIds) {
        return findAllActiveByIds(categoryIds, null);
    }

    @Override
    public List<Category> findAllActiveByIds(List<Long> categoryIds, FeedType feedType) {
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(
                findActiveByIdsSql(feedType),
                categoryParameters(categoryIds, feedType),
                (resultSet, rowNumber) -> toCategory(resultSet, feedType, true)
        );
    }

    @Override
    public void saveFeedType(long categoryId, FeedType feedType) {
        jdbcTemplate.update(
                """
                        INSERT INTO category_feed_types (category_id, feed_type)
                        VALUES (:categoryId, :feedType)
                        ON CONFLICT (category_id, feed_type) DO NOTHING
                        """,
                new MapSqlParameterSource()
                        .addValue("categoryId", categoryId)
                        .addValue("feedType", feedType.name())
        );
    }

    private String findAllActiveSql(FeedType feedType) {
        String feedTypeCondition = feedType == null ? "" : "  AND cft.feed_type = :feedType\n";
        return """
                SELECT c.id, c.slug, c.display_name, c.category_type,
                       c.display_order, c.is_active, cft.feed_type
                FROM categories c
                JOIN category_feed_types cft ON cft.category_id = c.id
                WHERE c.is_active = true
                """ + feedTypeCondition + """
                ORDER BY c.display_order, c.id, cft.feed_type
                """;
    }

    private String findActiveByIdsSql(FeedType feedType) {
        String feedTypeCondition = feedType == null ? "" : "  AND cft.feed_type = :feedType\n";
        return """
                SELECT c.id, c.slug, c.display_name, c.category_type,
                       c.display_order, c.is_active, cft.feed_type
                FROM categories c
                JOIN category_feed_types cft ON cft.category_id = c.id
                WHERE c.id IN (:categoryIds)
                  AND c.is_active = true
                """ + feedTypeCondition + """
                ORDER BY c.display_order, c.id, cft.feed_type
                """;
    }

    private MapSqlParameterSource categoryParameters(FeedType feedType) {
        return new MapSqlParameterSource()
                .addValue("feedType", feedType == null ? null : feedType.name());
    }

    private MapSqlParameterSource categoryParameters(
            List<Long> categoryIds,
            FeedType feedType
    ) {
        return new MapSqlParameterSource()
                .addValue("categoryIds", categoryIds)
                .addValue("feedType", feedType == null ? null : feedType.name());
    }

    private Category toCategory(
            ResultSet resultSet,
            FeedType defaultFeedType,
            boolean readFeedType
    ) throws SQLException {
        String feedType = readFeedType ? resultSet.getString("feed_type") : null;
        return Category.reconstitute(
                resultSet.getLong("id"),
                resultSet.getString("slug"),
                resultSet.getString("display_name"),
                CategoryType.valueOf(resultSet.getString("category_type")),
                feedType == null ? defaultFeedType : FeedType.valueOf(feedType),
                resultSet.getInt("display_order"),
                resultSet.getBoolean("is_active")
        );
    }

    private boolean isUniqueViolation(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof SQLException sqlException
                    && UNIQUE_VIOLATION_SQL_STATE.equals(sqlException.getSQLState())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private RuntimeException translateException(DataIntegrityViolationException exception) {
        if (isUniqueViolation(exception)) {
            return new DuplicateEntityException(
                    CategoryErrorCode.CATEGORY_ALREADY_EXISTS,
                    exception
            );
        }
        return exception;
    }
}

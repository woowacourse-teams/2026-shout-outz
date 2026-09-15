package com.shoutoutz.api.category.infrastructure;

import com.shoutoutz.api.category.domain.Category;
import com.shoutoutz.api.category.domain.CategoryErrorCode;
import com.shoutoutz.api.category.domain.CategoryRepository;
import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
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
            return insertCategory(category);
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
                (resultSet, rowNumber) -> toCategory(resultSet)
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
                (resultSet, rowNumber) -> toCategory(resultSet)
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
                        SELECT id, slug, display_name, category_type, display_order, is_active
                        FROM categories
                        WHERE id = :categoryId
                        """,
                Map.of("categoryId", categoryId),
                (resultSet, rowNumber) -> toCategory(resultSet)
        );
        return categories.stream().findFirst();
    }

    @Override
    public List<Category> findAllActive() {
        return jdbcTemplate.query(
                """
                        SELECT id, slug, display_name, category_type, display_order, is_active
                        FROM categories
                        WHERE is_active = true
                        ORDER BY display_order, id
                        """,
                Map.of(),
                (resultSet, rowNumber) -> toCategory(resultSet)
        );
    }

    @Override
    public List<Category> findAllActiveByIds(List<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(
                """
                        SELECT id, slug, display_name, category_type, display_order, is_active
                        FROM categories
                        WHERE id IN (:categoryIds)
                          AND is_active = true
                        """,
                Map.of("categoryIds", categoryIds),
                (resultSet, rowNumber) -> toCategory(resultSet)
        );
    }

    private Category toCategory(ResultSet resultSet) throws SQLException {
        return Category.reconstitute(
                resultSet.getLong("id"),
                resultSet.getString("slug"),
                resultSet.getString("display_name"),
                CategoryType.valueOf(resultSet.getString("category_type")),
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

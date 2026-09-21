package com.shoutoutz.api.feed.infrastructure;

import com.shoutoutz.api.feed.domain.Feed;
import com.shoutoutz.api.feed.domain.FeedRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedRepositoryImpl implements FeedRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Feed save(Feed feed) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO feeds (author_id, title, content, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?)
                        RETURNING id, author_id, title, content, created_at, updated_at, deleted_at
                        """,
                feedRowMapper(),
                feed.getAuthorId(),
                feed.getTitle(),
                feed.getContent(),
                Timestamp.from(feed.getCreatedAt()),
                Timestamp.from(feed.getUpdatedAt())
        );
    }

    @Override
    public Feed update(Feed feed) {
        return jdbcTemplate.queryForObject(
                """
                        UPDATE feeds
                        SET title = ?, content = ?, updated_at = ?, deleted_at = ?
                        WHERE id = ?
                        RETURNING id, author_id, title, content, created_at, updated_at, deleted_at
                        """,
                feedRowMapper(),
                feed.getTitle(),
                feed.getContent(),
                Timestamp.from(feed.getUpdatedAt()),
                toTimestamp(feed.getDeletedAt()),
                feed.getId()
        );
    }

    @Override
    public Optional<Feed> findActiveById(long feedId) {
        return jdbcTemplate.query(
                        """
                                SELECT id, author_id, title, content, created_at, updated_at, deleted_at
                                FROM feeds
                                WHERE id = ?
                                  AND deleted_at IS NULL
                                """,
                        feedRowMapper(),
                        feedId
                )
                .stream()
                .findFirst();
    }

    @Override
    public void saveCategories(long feedId, List<Long> categoryIds) {
        jdbcTemplate.update("DELETE FROM feed_categories WHERE feed_id = ?", feedId);
        categoryIds.forEach(categoryId -> jdbcTemplate.update(
                "INSERT INTO feed_categories (feed_id, category_id) VALUES (?, ?)",
                feedId,
                categoryId
        ));
    }

    @Override
    public void saveMedia(long feedId, List<Long> mediaIds) {
        jdbcTemplate.update("DELETE FROM feed_media WHERE feed_id = ?", feedId);
        for (int displayOrder = 0; displayOrder < mediaIds.size(); displayOrder++) {
            jdbcTemplate.update(
                    """
                            INSERT INTO feed_media (feed_id, media_metadata_id, display_order)
                            VALUES (?, ?, ?)
                            """,
                    feedId,
                    mediaIds.get(displayOrder),
                    displayOrder
            );
        }
    }

    private RowMapper<Feed> feedRowMapper() {
        return (resultSet, rowNumber) -> Feed.reconstitute(
                resultSet.getLong("id"),
                resultSet.getLong("author_id"),
                resultSet.getString("title"),
                resultSet.getString("content"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant(),
                toInstant(resultSet, "deleted_at")
        );
    }

    private Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant();
    }

    private Timestamp toTimestamp(Instant instant) {
        if (instant == null) {
            return null;
        }
        return Timestamp.from(instant);
    }
}

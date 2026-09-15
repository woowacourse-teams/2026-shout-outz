package com.shoutoutz.api.post.infrastructure;

import com.shoutoutz.api.post.domain.Post;
import com.shoutoutz.api.post.domain.PostRepository;
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
public class PostRepositoryImpl implements PostRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Post save(Post post) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO posts (author_id, content, created_at, updated_at)
                        VALUES (?, ?, ?, ?)
                        RETURNING id, author_id, content, created_at, updated_at, deleted_at
                        """,
                postRowMapper(),
                post.getAuthorId(),
                post.getContent(),
                Timestamp.from(post.getCreatedAt()),
                Timestamp.from(post.getUpdatedAt())
        );
    }

    @Override
    public Post update(Post post) {
        return jdbcTemplate.queryForObject(
                """
                        UPDATE posts
                        SET content = ?, updated_at = ?, deleted_at = ?
                        WHERE id = ?
                        RETURNING id, author_id, content, created_at, updated_at, deleted_at
                        """,
                postRowMapper(),
                post.getContent(),
                Timestamp.from(post.getUpdatedAt()),
                toTimestamp(post.getDeletedAt()),
                post.getId()
        );
    }

    @Override
    public Optional<Post> findActiveById(long postId) {
        return jdbcTemplate.query(
                        """
                                SELECT id, author_id, content, created_at, updated_at, deleted_at
                                FROM posts
                                WHERE id = ?
                                  AND deleted_at IS NULL
                                """,
                        postRowMapper(),
                        postId
                )
                .stream()
                .findFirst();
    }

    @Override
    public void saveCategories(long postId, List<Long> categoryIds) {
        jdbcTemplate.update("DELETE FROM post_categories WHERE post_id = ?", postId);
        categoryIds.forEach(categoryId -> jdbcTemplate.update(
                "INSERT INTO post_categories (post_id, category_id) VALUES (?, ?)",
                postId,
                categoryId
        ));
    }

    @Override
    public void saveMedia(long postId, List<Long> mediaIds) {
        jdbcTemplate.update("DELETE FROM post_media WHERE post_id = ?", postId);
        for (int displayOrder = 0; displayOrder < mediaIds.size(); displayOrder++) {
            jdbcTemplate.update(
                    """
                            INSERT INTO post_media (post_id, media_metadata_id, display_order)
                            VALUES (?, ?, ?)
                            """,
                    postId,
                    mediaIds.get(displayOrder),
                    displayOrder
            );
        }
    }

    private RowMapper<Post> postRowMapper() {
        return (resultSet, rowNumber) -> Post.reconstitute(
                resultSet.getLong("id"),
                resultSet.getLong("author_id"),
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

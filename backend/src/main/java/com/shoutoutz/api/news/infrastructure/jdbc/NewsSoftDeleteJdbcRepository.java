package com.shoutoutz.api.news.infrastructure.jdbc;

import java.sql.Timestamp;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 소식의 원자적 소프트 삭제를 담당한다.
 */
@Repository
@RequiredArgsConstructor
public class NewsSoftDeleteJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean softDelete(long newsId, Instant deletedAt) {
        return !jdbcTemplate.query(
                        """
                                UPDATE news
                                SET deleted_at = ?, updated_at = ?
                                WHERE id = ?
                                  AND deleted_at IS NULL
                                RETURNING id
                                """,
                        (resultSet, rowNumber) -> resultSet.getLong("id"),
                        Timestamp.from(deletedAt),
                        Timestamp.from(deletedAt),
                        newsId
                )
                .isEmpty();
    }
}

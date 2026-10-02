package com.shoutoutz.api.home.infrastructure;

import com.shoutoutz.api.home.application.HomeStatisticsQueryRepository;
import com.shoutoutz.api.home.application.dto.HomeStatisticsCounts;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HomeStatisticsQueryRepositoryImpl implements HomeStatisticsQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public HomeStatisticsCounts find(Instant now) {
        String sql = """
                SELECT
                    (
                        SELECT COUNT(*)
                        FROM projects p
                        WHERE p.approval_status = 'APPROVED'
                          AND p.deleted_at IS NULL
                    ) AS project_count,
                    (
                        SELECT COUNT(*)
                        FROM feeds f
                        WHERE f.deleted_at IS NULL
                    ) AS feed_count,
                    (
                        SELECT COUNT(*)
                        FROM news n
                        WHERE n.type = 'EVENT'
                          AND n.deleted_at IS NULL
                          AND n.event_start_at <= :now
                          AND n.event_end_at >= :now
                    ) AS ongoing_event_count
                """;

        return jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("now", OffsetDateTime.ofInstant(now, ZoneOffset.UTC)),
                (resultSet, rowNumber) -> new HomeStatisticsCounts(
                        resultSet.getLong("project_count"),
                        resultSet.getLong("feed_count"),
                        resultSet.getLong("ongoing_event_count")
                )
        );
    }
}

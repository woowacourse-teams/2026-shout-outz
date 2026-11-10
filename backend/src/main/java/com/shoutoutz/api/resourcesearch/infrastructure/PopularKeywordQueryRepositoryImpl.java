package com.shoutoutz.api.resourcesearch.infrastructure;

import com.shoutoutz.api.resourcesearch.application.PopularKeywordQueryRepository;
import com.shoutoutz.api.resourcesearch.domain.KeywordSearchCount;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PopularKeywordQueryRepositoryImpl implements PopularKeywordQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<KeywordSearchCount> findCounts(LocalDate searchedOnFrom, int minSearchCount) {
        return jdbcTemplate.query(
                """
                        WITH counts AS (
                            SELECT keyword, count(*) AS search_count, max(last_searched_at) AS last_searched_at
                            FROM search_keyword_days
                            WHERE searched_on >= :searchedOnFrom
                            GROUP BY keyword
                            HAVING count(*) >= :minSearchCount
                        ),
                        whitelist AS (
                            SELECT DISTINCT ON (normalized) normalized, display
                            FROM (
                                SELECT lower(regexp_replace(btrim(name), '\\s+', ' ', 'g')) AS normalized,
                                       name AS display, 0 AS priority
                                FROM resource_topic_catalog
                                UNION ALL
                                SELECT lower(regexp_replace(btrim(tag), '\\s+', ' ', 'g')), tag, 1
                                FROM resource_documents d, unnest(d.tech_tags) AS tag
                                WHERE d.deleted_at IS NULL
                            ) names
                            WHERE normalized IN (SELECT keyword FROM counts)
                            ORDER BY normalized, priority, display
                        )
                        SELECT w.display, c.search_count, c.last_searched_at
                        FROM counts c
                        JOIN whitelist w ON w.normalized = c.keyword
                        """,
                new MapSqlParameterSource()
                        .addValue("searchedOnFrom", searchedOnFrom)
                        .addValue("minSearchCount", minSearchCount),
                (resultSet, rowNumber) -> new KeywordSearchCount(
                        resultSet.getString("display"),
                        resultSet.getLong("search_count"),
                        resultSet.getTimestamp("last_searched_at").toInstant()
                )
        );
    }
}

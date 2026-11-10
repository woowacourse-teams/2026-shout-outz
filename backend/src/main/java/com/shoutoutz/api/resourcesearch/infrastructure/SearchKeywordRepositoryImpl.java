package com.shoutoutz.api.resourcesearch.infrastructure;

import com.shoutoutz.api.resourcesearch.domain.SearchKeyword;
import com.shoutoutz.api.resourcesearch.domain.SearchKeywordRepository;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SearchKeywordRepositoryImpl implements SearchKeywordRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void record(SearchKeyword keyword, VisitorKey visitorKey, LocalDate searchedOn, Instant searchedAt) {
        jdbcTemplate.update(
                """
                        INSERT INTO search_keyword_days (keyword, visitor_key_hash, searched_on, last_searched_at)
                        VALUES (:keyword, :visitorKeyHash, :searchedOn, :searchedAt)
                        ON CONFLICT (keyword, visitor_key_hash, searched_on) DO UPDATE
                        SET last_searched_at = GREATEST(search_keyword_days.last_searched_at, EXCLUDED.last_searched_at)
                        """,
                new MapSqlParameterSource()
                        .addValue("keyword", keyword.value())
                        .addValue("visitorKeyHash", visitorKey.hash())
                        .addValue("searchedOn", searchedOn)
                        .addValue("searchedAt", Timestamp.from(searchedAt))
        );
    }
}

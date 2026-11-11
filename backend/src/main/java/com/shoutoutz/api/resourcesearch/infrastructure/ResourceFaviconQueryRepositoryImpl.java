package com.shoutoutz.api.resourcesearch.infrastructure;

import com.shoutoutz.api.resourcesearch.application.ResourceFaviconQueryRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ResourceFaviconQueryRepositoryImpl implements ResourceFaviconQueryRepository {

    private static final String FIND_FAVICON_URLS_SQL = """
            SELECT ref.document_id, preview.favicon_url
            FROM resource_link_preview_refs ref
            JOIN link_preview_cache preview ON preview.id = ref.cache_id
            WHERE ref.document_id IN (:resourceIds)
              AND preview.status = 'READY'
              AND preview.favicon_url IS NOT NULL
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Map<Long, String> findFaviconUrls(Collection<Long> resourceIds) {
        if (resourceIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> faviconUrls = new HashMap<>();
        jdbcTemplate.query(
                FIND_FAVICON_URLS_SQL,
                new MapSqlParameterSource("resourceIds", resourceIds),
                resultSet -> {
                    faviconUrls.put(resultSet.getLong("document_id"), resultSet.getString("favicon_url"));
                }
        );
        return faviconUrls;
    }
}

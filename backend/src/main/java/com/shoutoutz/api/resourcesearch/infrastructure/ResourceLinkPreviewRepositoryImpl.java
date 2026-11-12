package com.shoutoutz.api.resourcesearch.infrastructure;

import com.shoutoutz.api.resourcesearch.application.ResourceLinkPreviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ResourceLinkPreviewRepositoryImpl implements ResourceLinkPreviewRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public int linkUnlinkedResources(int limit) {
        return jdbcTemplate.update("""
                WITH targets AS (
                    SELECT d.id AS document_id,
                           d.url,
                           encode(sha256(convert_to(d.url, 'UTF8')), 'hex') AS url_hash
                    FROM resource_documents d
                    LEFT JOIN resource_link_preview_refs r ON r.document_id = d.id
                    LEFT JOIN link_preview_cache linked ON linked.id = r.cache_id
                    WHERE d.deleted_at IS NULL
                      AND (r.document_id IS NULL OR linked.url <> d.url)
                    ORDER BY d.id
                    LIMIT :limit
                ),
                created AS (
                    INSERT INTO link_preview_cache (url_hash, url)
                    SELECT DISTINCT url_hash, url FROM targets
                    ON CONFLICT (url_hash) DO NOTHING
                    RETURNING id, url
                )
                INSERT INTO resource_link_preview_refs (document_id, cache_id)
                SELECT t.document_id, COALESCE(created.id, existing.id)
                FROM targets t
                LEFT JOIN created ON created.url = t.url
                LEFT JOIN link_preview_cache existing ON existing.url_hash = t.url_hash AND existing.url = t.url
                WHERE COALESCE(created.id, existing.id) IS NOT NULL
                ON CONFLICT (document_id) DO UPDATE SET cache_id = EXCLUDED.cache_id
                """, new MapSqlParameterSource("limit", limit));
    }
}

package com.shoutoutz.api.feed.infrastructure;

import com.shoutoutz.api.feed.application.dto.LinkPreview;
import com.shoutoutz.api.feed.application.dto.LinkPreviewMetadata;
import com.shoutoutz.api.feed.application.FeedLinkPreviewRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedLinkPreviewRepositoryImpl implements FeedLinkPreviewRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void link(long feedId, String url) {
        String hash = sha256(url);
        jdbcTemplate.update("""
                INSERT INTO link_preview_cache (url_hash, url)
                VALUES (:hash, :url)
                ON CONFLICT (url_hash) DO NOTHING
                """, Map.of("hash", hash, "url", url));
        CacheIdentity cache = jdbcTemplate.queryForObject("""
                SELECT id, url FROM link_preview_cache WHERE url_hash = :hash
                """, Map.of("hash", hash), (rs, rowNum) ->
                new CacheIdentity(rs.getLong("id"), rs.getString("url")));
        if (cache == null || !cache.url().equals(url)) {
            throw new IllegalStateException("링크 미리보기 URL 해시 충돌");
        }
        jdbcTemplate.update("""
                INSERT INTO feed_link_preview_refs (feed_id, cache_id)
                VALUES (:feedId, :cacheId)
                ON CONFLICT (feed_id) DO UPDATE SET cache_id = EXCLUDED.cache_id
                """, Map.of("feedId", feedId, "cacheId", cache.id()));
    }

    @Override
    public void unlink(long feedId) {
        jdbcTemplate.update("DELETE FROM feed_link_preview_refs WHERE feed_id = :feedId",
                Map.of("feedId", feedId));
    }

    @Override
    public boolean hasReference(long feedId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM feed_link_preview_refs WHERE feed_id = :feedId)
                """, Map.of("feedId", feedId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public Map<Long, LinkPreview> findByFeedIds(List<Long> feedIds) {
        if (feedIds.isEmpty()) {
            return Map.of();
        }
        return jdbcTemplate.query("""
                SELECT r.feed_id, c.url, c.title, c.description, c.image_url, c.site_name
                FROM feed_link_preview_refs r
                JOIN link_preview_cache c ON c.id = r.cache_id
                WHERE r.feed_id IN (:feedIds)
                """, Map.of("feedIds", feedIds), rs -> {
            Map<Long, LinkPreview> previews = new HashMap<>();
            while (rs.next()) {
                previews.put(rs.getLong("feed_id"), new LinkPreview(
                        rs.getString("url"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("image_url"),
                        rs.getString("site_name")
                ));
            }
            return previews;
        });
    }

    /** 하나의 SQL 문에서 작업을 선점해 여러 서버가 같은 URL을 동시에 가져오지 않게 한다. */
    @Override
    public Optional<FetchJob> claimDue(Instant now, Instant staleBefore) {
        Instant leaseUntil = now.plusSeconds(45);
        List<FetchJob> claimed = jdbcTemplate.query("""
                UPDATE link_preview_cache c
                SET status = 'PROCESSING',
                    attempts = c.attempts + 1,
                    lease_until = :leaseUntil
                WHERE c.id = (
                    SELECT candidate.id
                    FROM link_preview_cache candidate
                    WHERE (
                        (candidate.status IN ('PENDING', 'FAILED')
                            AND candidate.next_attempt_at <= :now)
                        OR (candidate.status = 'PROCESSING'
                            AND candidate.lease_until < :now)
                        OR (candidate.status = 'READY'
                            AND candidate.fetched_at < :staleBefore)
                    )
                    AND EXISTS (
                        SELECT 1
                        FROM feed_link_preview_refs r
                        JOIN feeds f ON f.id = r.feed_id
                        WHERE r.cache_id = candidate.id AND f.deleted_at IS NULL
                    )
                    ORDER BY candidate.next_attempt_at NULLS LAST, candidate.id
                    LIMIT 1
                    FOR UPDATE SKIP LOCKED
                )
                RETURNING c.id, c.url, c.attempts
                """, new MapSqlParameterSource()
                        .addValue("now", now)
                        .addValue("staleBefore", staleBefore)
                        .addValue("leaseUntil", leaseUntil),
                (rs, rowNum) -> new FetchJob(
                        rs.getLong("id"), rs.getString("url"), rs.getInt("attempts")));
        return claimed.stream().findFirst();
    }

    @Override
    public void complete(FetchJob job, LinkPreviewMetadata metadata, Instant now) {
        jdbcTemplate.update("""
                UPDATE link_preview_cache
                SET status = 'READY', title = :title, description = :description,
                    image_url = :imageUrl, site_name = :siteName, fetched_at = :now,
                    next_attempt_at = NULL, lease_until = NULL, attempts = 0
                WHERE id = :id AND status = 'PROCESSING' AND attempts = :attempts
                """, new MapSqlParameterSource()
                        .addValue("id", job.id())
                        .addValue("attempts", job.attempts())
                        .addValue("title", metadata.title())
                        .addValue("description", metadata.description())
                        .addValue("imageUrl", metadata.imageUrl())
                        .addValue("siteName", metadata.siteName())
                        .addValue("now", now));
    }

    @Override
    public void fail(FetchJob job, Instant nextAttemptAt) {
        jdbcTemplate.update("""
                UPDATE link_preview_cache
                SET status = 'FAILED', next_attempt_at = :nextAttemptAt, lease_until = NULL
                WHERE id = :id AND status = 'PROCESSING' AND attempts = :attempts
                """, new MapSqlParameterSource()
                        .addValue("id", job.id())
                        .addValue("attempts", job.attempts())
                        .addValue("nextAttemptAt", nextAttemptAt));
    }

    /** 기존 피드를 작은 단위로 훑는다. 호출자의 트랜잭션에서 피드 행을 잠근다. */
    @Override
    public List<FeedContent> findFeedsAfter(long afterId, int limit) {
        return jdbcTemplate.query("""
                SELECT id, content FROM feeds
                WHERE id > :afterId AND deleted_at IS NULL
                ORDER BY id
                LIMIT :limit
                FOR UPDATE
                """, Map.of("afterId", afterId, "limit", limit),
                (rs, rowNum) -> new FeedContent(rs.getLong("id"), rs.getString("content")));
    }

    @Override
    public int deleteUnusedBefore(Instant cutoff) {
        return jdbcTemplate.update("""
                DELETE FROM link_preview_cache c
                WHERE NOT EXISTS (
                    SELECT 1 FROM feed_link_preview_refs r WHERE r.cache_id = c.id
                )
                AND (
                    (c.status = 'READY' AND c.fetched_at < :cutoff)
                    OR (c.status IN ('PENDING', 'FAILED') AND c.next_attempt_at < :cutoff)
                    OR (c.status = 'PROCESSING' AND c.lease_until < :cutoff)
                )
                """, Map.of("cutoff", cutoff));
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private record CacheIdentity(long id, String url) {
    }

}

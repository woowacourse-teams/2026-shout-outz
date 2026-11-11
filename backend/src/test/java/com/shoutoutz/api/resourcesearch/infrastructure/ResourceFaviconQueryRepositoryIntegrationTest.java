package com.shoutoutz.api.resourcesearch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcesearch.application.ResourceFaviconQueryRepository;
import com.shoutoutz.api.resourcesearch.support.ResourceDocumentFixture;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ResourceFaviconQueryRepositoryIntegrationTest {

    @Autowired
    private ResourceFaviconQueryRepository resourceFaviconQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ResourceDocumentFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ResourceDocumentFixture(jdbcTemplate);
    }

    @Test
    @DisplayName("마지막으로 받은 파비콘을 수집 상태와 관계없이 자료 ID별로 주고, 받은 적이 없으면 뺀다")
    void findsLastCollectedFavicons() {
        long ready = fixture.document("blog-post").save();
        long refetching = fixture.document("blog-post").save();
        long failed = fixture.document("blog-post").save();
        long neverCollected = fixture.document("blog-post").save();
        long notLinked = fixture.document("blog-post").save();
        link(ready, "READY", "https://jun.dev/favicon.png");
        link(refetching, "PROCESSING", "https://refetching.dev/favicon.png");
        link(failed, "FAILED", "https://down.dev/favicon.png");
        link(neverCollected, "PENDING", null);

        assertThat(resourceFaviconQueryRepository.findFaviconUrls(
                List.of(ready, refetching, failed, neverCollected, notLinked)))
                .containsOnly(
                        Map.entry(ready, "https://jun.dev/favicon.png"),
                        Map.entry(refetching, "https://refetching.dev/favicon.png"),
                        Map.entry(failed, "https://down.dev/favicon.png")
                );
        assertThat(resourceFaviconQueryRepository.findFaviconUrls(List.of())).isEmpty();
    }

    private void link(long documentId, String status, String faviconUrl) {
        String url = "https://example.com/" + UUID.randomUUID();
        Long cacheId = jdbcTemplate.queryForObject("""
                INSERT INTO link_preview_cache (url_hash, url, status, favicon_url)
                VALUES (md5(?) || md5(?), ?, ?, ?)
                RETURNING id
                """, Long.class, url, url, url, status, faviconUrl);
        jdbcTemplate.update(
                "INSERT INTO resource_link_preview_refs (document_id, cache_id) VALUES (?, ?)", documentId, cacheId);
    }
}

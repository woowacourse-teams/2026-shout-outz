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
    @DisplayName("수집이 끝난 링크 미리보기의 파비콘만 자료 ID별로 준다")
    void findsFaviconsOfReadyPreviews() {
        long ready = fixture.document("blog-post").save();
        long pending = fixture.document("blog-post").save();
        long withoutFavicon = fixture.document("blog-post").save();
        long notLinked = fixture.document("blog-post").save();
        link(ready, "READY", "https://jun.dev/favicon.png");
        link(pending, "PENDING", "https://pending.dev/favicon.png");
        link(withoutFavicon, "READY", null);

        assertThat(resourceFaviconQueryRepository.findFaviconUrls(List.of(ready, pending, withoutFavicon, notLinked)))
                .containsExactlyEntriesOf(Map.of(ready, "https://jun.dev/favicon.png"));
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

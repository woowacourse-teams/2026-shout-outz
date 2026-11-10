package com.shoutoutz.api.resourcesearch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcesearch.application.ResourceLinkPreviewRepository;
import com.shoutoutz.api.resourcesearch.support.ResourceDocumentFixture;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
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
class ResourceLinkPreviewRepositoryIntegrationTest {

    @Autowired
    private ResourceLinkPreviewRepository resourceLinkPreviewRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ResourceDocumentFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ResourceDocumentFixture(jdbcTemplate);
        jdbcTemplate.update("UPDATE resource_documents SET deleted_at = now() WHERE deleted_at IS NULL");
    }

    @Test
    @DisplayName("연결되지 않은 자료의 원문 주소를 수집 대기 캐시로 만들고 연결한다")
    void linksUnlinkedResourceToPendingCache() {
        long id = fixture.document("blog-post").save();
        String url = urlOf(id);

        int linked = resourceLinkPreviewRepository.linkUnlinkedResources(20);

        assertThat(linked).isEqualTo(1);
        assertThat(jdbcTemplate.queryForMap("""
                SELECT c.url, c.url_hash, c.status
                FROM resource_link_preview_refs r
                JOIN link_preview_cache c ON c.id = r.cache_id
                WHERE r.document_id = ?
                """, id))
                .containsEntry("url", url)
                .containsEntry("url_hash", sha256(url))
                .containsEntry("status", "PENDING");
    }

    @Test
    @DisplayName("피드가 이미 만든 같은 주소의 캐시는 새로 만들지 않고 함께 쓴다")
    void reusesExistingCacheForSameUrl() {
        long id = fixture.document("blog-post").save();
        String url = urlOf(id);
        Long cacheId = jdbcTemplate.queryForObject("""
                INSERT INTO link_preview_cache (url_hash, url, status, title, fetched_at)
                VALUES (?, ?, 'READY', '이미 모은 제목', now())
                RETURNING id
                """, Long.class, sha256(url), url);

        resourceLinkPreviewRepository.linkUnlinkedResources(20);

        assertThat(cacheIdOf(id)).isEqualTo(cacheId);
        assertThat(cacheCountOf(url)).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 주소를 가진 자료 여러 개는 캐시 하나를 함께 쓴다")
    void sharesOneCacheBetweenResourcesWithSameUrl() {
        long first = fixture.document("blog-post").save();
        long second = fixture.document("github-pr").save();
        jdbcTemplate.update("UPDATE resource_documents SET url = 'https://example.com/same' WHERE id IN (?, ?)", first, second);

        resourceLinkPreviewRepository.linkUnlinkedResources(20);

        assertThat(cacheIdOf(first)).isEqualTo(cacheIdOf(second));
        assertThat(cacheCountOf("https://example.com/same")).isEqualTo(1);
    }

    @Test
    @DisplayName("한 번에 요청한 개수까지만 자료 ID 순서로 연결한다")
    void linksUpToLimitInIdOrder() {
        long first = fixture.document("blog-post").save();
        long second = fixture.document("blog-post").save();
        fixture.document("blog-post").save();

        int linked = resourceLinkPreviewRepository.linkUnlinkedResources(2);

        assertThat(linked).isEqualTo(2);
        assertThat(linkedDocumentIds()).containsExactly(first, second);
    }

    @Test
    @DisplayName("이미 연결된 자료는 다시 연결하지 않는다")
    void skipsAlreadyLinkedResources() {
        fixture.document("blog-post").save();
        resourceLinkPreviewRepository.linkUnlinkedResources(20);

        int linked = resourceLinkPreviewRepository.linkUnlinkedResources(20);

        assertThat(linked).isZero();
    }

    @Test
    @DisplayName("삭제된 자료는 연결하지 않는다")
    void skipsDeletedResources() {
        fixture.document("blog-post").deleted().save();

        int linked = resourceLinkPreviewRepository.linkUnlinkedResources(20);

        assertThat(linked).isZero();
    }

    @Test
    @DisplayName("원문 주소가 바뀐 자료는 새 주소의 캐시로 다시 연결한다")
    void relinksWhenUrlChanged() {
        long id = fixture.document("blog-post").save();
        resourceLinkPreviewRepository.linkUnlinkedResources(20);
        jdbcTemplate.update("UPDATE resource_documents SET url = 'https://example.com/moved' WHERE id = ?", id);

        int linked = resourceLinkPreviewRepository.linkUnlinkedResources(20);

        assertThat(linked).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT c.url FROM resource_link_preview_refs r JOIN link_preview_cache c ON c.id = r.cache_id
                WHERE r.document_id = ?
                """, String.class, id)).isEqualTo("https://example.com/moved");
    }

    private String urlOf(long id) {
        return jdbcTemplate.queryForObject("SELECT url FROM resource_documents WHERE id = ?", String.class, id);
    }

    private Long cacheIdOf(long documentId) {
        return jdbcTemplate.queryForObject(
                "SELECT cache_id FROM resource_link_preview_refs WHERE document_id = ?", Long.class, documentId);
    }

    private Integer cacheCountOf(String url) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM link_preview_cache WHERE url = ?", Integer.class, url);
    }

    private List<Long> linkedDocumentIds() {
        return jdbcTemplate.queryForList(
                "SELECT document_id FROM resource_link_preview_refs ORDER BY document_id", Long.class);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}

package com.shoutoutz.api.resourcesearch.application;

import static com.shoutoutz.api.resourcesearch.support.ResourceSearchFixture.MODEL;
import static com.shoutoutz.api.resourcesearch.support.ResourceSearchFixture.vector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.embedding.EmbeddingClient;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import com.shoutoutz.api.resourcesearch.support.ResourceSearchFixture;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest(properties = "logging.level.com.shoutoutz.api.resourcesearch=DEBUG")
@Transactional
class ResourceSearchServiceIntegrationTest {

    @Autowired
    private ResourceSearchService resourceSearchService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmbeddingClient embeddingClient;

    @MockitoSpyBean
    private ResourceSearchRepository resourceSearchRepository;

    private ResourceSearchFixture fixture;
    private float[] queryVector;

    @BeforeEach
    void setUp() {
        fixture = new ResourceSearchFixture(jdbcTemplate);
        queryVector = vector(1, 0);
        when(embeddingClient.embed(any())).thenReturn(queryVector);
        when(embeddingClient.modelId()).thenReturn(MODEL);
    }

    @Test
    void 소개와_경험을_문서로_묶고_원본_식별자와_관련_조각을_반환한다() {
        long document = fixture.document("github-pr", "BE", 42L, "example/repo", "작업 유실 방지");
        long intro = fixture.intro(document, vector(0.8f, 0.6f));
        long experience = fixture.chunk(document, 1, "experience", "문제: 작업 유실\n해결: DB에 보관",
                vector(1, 0), MODEL);
        long unlinked = fixture.document("blog-post", null, null, "example/unlinked", "재시도 기록");
        fixture.intro(unlinked, vector(0.6f, 0.8f));

        List<SearchHit> hits = resourceSearchService.search("작업이 유실되지 않도록 처리한 사례", "BE", 2);

        assertThat(hits).extracting(SearchHit::documentId).containsExactly(document, unlinked);
        assertThat(hits.getFirst().similarityScore()).isCloseTo(1, within(0.000001));
        assertThat(hits.getFirst().projectId()).isEqualTo(42L);
        assertThat(hits.getFirst().externalKey()).isEqualTo(jdbcTemplate.queryForObject(
                "SELECT external_key FROM resource_documents WHERE id = ?", String.class, document));
        assertThat(hits.getFirst().matchedChunks()).extracting(MatchedChunk::chunkId)
                .containsExactly(experience, intro);
        assertThat(hits.getLast().projectId()).isNull();
        assertThat(hits.getLast().repo()).isEqualTo("example/unlinked");
        assertThat(hits.getLast().track()).isNull();
        assertThatThrownBy(() -> hits.clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> hits.getFirst().matchedChunks().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void 실제_DB에서_후보를_확대해_요청한_문서수를_채운다() {
        for (int i = 0; i < 10; i++) {
            long document = fixture.document("BE");
            fixture.intro(document, vector(1, 0));
            for (int chunk = 1; chunk <= 3; chunk++) {
                fixture.chunk(document, chunk, "experience", "문제: 유실\n해결: 재시도", vector(1, 0), MODEL);
            }
        }

        List<SearchHit> hits = resourceSearchService.search("작업 복구", "BE", 10);

        assertThat(hits).hasSize(10);
        assertThat(hits).allSatisfy(hit -> assertThat(hit.matchedChunks()).hasSize(4));
        verify(resourceSearchRepository).findNearest(queryVector, MODEL, "BE", 20);
        verify(resourceSearchRepository).findNearest(queryVector, MODEL, "BE", 40);
        verify(embeddingClient, times(1)).embed("작업 복구");
    }

    @Test
    void 공통과_미분류는_포함하고_다른_트랙과_삭제_및_다른_모델은_제외한다() {
        fixture.intro(fixture.document("FE"), vector(1, 0));
        long backend = fixture.document("BE");
        fixture.intro(backend, vector(1, 0));
        long common = fixture.document("ALL");
        fixture.intro(common, vector(0.8f, 0.6f));
        long unknown = fixture.document(null);
        fixture.intro(unknown, vector(0.6f, 0.8f));
        long deleted = fixture.document("BE");
        fixture.intro(deleted, vector(1, 0));
        jdbcTemplate.update("UPDATE resource_documents SET deleted_at = now() WHERE id = ?", deleted);
        fixture.chunk(fixture.document("BE"), 0, "intro", "다른 모델", vector(1, 0), "other@1024");

        List<SearchHit> hits = resourceSearchService.search("복구 경험", "BE", 10);

        assertThat(hits).extracting(SearchHit::documentId).containsExactly(backend, common, unknown);
        assertThat(hits).extracting(SearchHit::track).containsExactly("BE", "ALL", null);
    }

    @Test
    void 소개만_있는_자료와_소개가_더_가까운_자료도_계약대로_반환한다() {
        long mixed = fixture.document("BE");
        fixture.intro(mixed, vector(1, 0));
        fixture.chunk(mixed, 1, "experience", "문제: 실패\n해결: 재시도", vector(0.8f, 0.6f), MODEL);
        long talk = fixture.document("youtube-tekotalk", null, null, null, "재시도 개념");
        fixture.intro(talk, vector(0.6f, 0.8f));

        List<SearchHit> hits = resourceSearchService.search("재시도", null, 10);

        assertThat(hits.getFirst().matchedChunks()).extracting(MatchedChunk::chunkKind)
                .containsExactly("intro", "experience");
        assertThat(hits.getFirst().similarityScore()).isCloseTo(1, within(0.000001));
        assertThat(hits.getLast().documentId()).isEqualTo(talk);
        assertThat(hits.getLast().repo()).isNull();
        assertThat(hits.getLast().matchedChunks()).extracting(MatchedChunk::chunkKind).containsExactly("intro");
    }

    @Test
    void 원천이_다르면_같은_externalKey의_문서를_합치지_않는다() {
        long pr = fixture.document("github-pr", "BE", null, null, "PR");
        long blog = fixture.document("blog-post", "BE", null, null, "블로그");
        jdbcTemplate.update("UPDATE resource_documents SET external_key = 'shared-key' WHERE id IN (?, ?)", pr, blog);
        fixture.intro(pr, vector(1, 0));
        fixture.intro(blog, vector(0.8f, 0.6f));

        List<SearchHit> hits = resourceSearchService.search("경험", "BE", 10);

        assertThat(hits).extracting(SearchHit::externalKey).containsExactly("shared-key", "shared-key");
        assertThat(hits).extracting(SearchHit::source).containsExactly("github-pr", "blog-post");
    }

    @Test
    void 자료가_없으면_성공한_빈_결과를_반환한다() {
        assertThat(resourceSearchService.search("경험", "BE", 10)).isEmpty();
    }
}

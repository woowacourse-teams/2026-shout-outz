package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.embedding.EmbeddingClient;
import com.shoutoutz.api.common.embedding.EmbeddingException;
import com.shoutoutz.api.common.exception.custom.ServiceUnavailableException;
import com.shoutoutz.api.resourcesearch.application.dto.LexicalMatch;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.application.dto.SearchCandidates;
import com.shoutoutz.api.resourcesearch.application.dto.RankedResource;
import com.shoutoutz.api.resourcesearch.application.dto.SectionedSearchHits;
import com.shoutoutz.api.resourcesearch.domain.ResourceSearchErrorCode;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class SectionedResourceSearchServiceTest {

    private static final String MODEL = "model@1024";
    private static final String QUERY = "동시성";

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private ResourceSearchRepository resourceSearchRepository;

    @Mock
    private LexicalChunkSearch lexicalChunkSearch;

    private SectionedResourceSearchService service;

    private float[] vector;
    private List<ResourceSearchCandidate> pool;
    private List<ResourceSearchCandidate> lexicalOnly;
    private Map<Long, String> sources;
    private long nextId;
    private long nextChunkId;

    @BeforeEach
    void setUp() {
        vector = new float[1024];
        vector[0] = 1;
        pool = new ArrayList<>();
        sources = new HashMap<>();
        nextId = 1;
        nextChunkId = 1;
        lexicalOnly = new ArrayList<>();
        service = new SectionedResourceSearchService(
                embeddingClient, new HybridResourceRanker(resourceSearchRepository, lexicalChunkSearch), lexicalChunkSearch
        );
    }

    @Test
    @DisplayName("검색 결과를 원천에 따라 블로그, 문서, 테코톡 섹션으로 나누고 연관도 순서를 지킨다")
    void splitsHitsIntoSectionsInScoreOrder() {
        long blog = add("blog-post", 0.95);
        long pr = add("github-pr", 0.9);
        long talk = add("youtube-tekotalk", 0.85);
        long wiki = add("github-wiki", 0.8);
        givenRepositoryReturnsTopOfPool();

        SectionedSearchHits result = service.search(QUERY, null);

        assertThat(ids(result, ResourceSection.BLOG)).containsExactly(blog);
        assertThat(ids(result, ResourceSection.DOCUMENT)).containsExactly(pr, wiki);
        assertThat(ids(result, ResourceSection.TALK)).containsExactly(talk);
    }

    @Test
    @DisplayName("섹션마다 상한까지만 담는다")
    void capsEachSection() {
        addMany("blog-post", 25, 0.9);
        addMany("github-pr", 25, 0.8);
        addMany("youtube-tekotalk", 10, 0.7);
        givenRepositoryReturnsTopOfPool();

        SectionedSearchHits result = service.search(QUERY, null);

        assertThat(result.hits(ResourceSection.BLOG)).hasSize(20);
        assertThat(result.hits(ResourceSection.DOCUMENT)).hasSize(20);
        assertThat(result.hits(ResourceSection.TALK)).hasSize(6);
    }

    @Test
    @DisplayName("블로그 섹션이 차지 않으면 후보 범위를 넓히되 조회와 임베딩은 한 번만 한다")
    void widensCandidatesUntilBlogSectionIsFullWithSingleQuery() {
        addMany("github-pr", 100, 0.9);
        addMany("blog-post", 20, 0.5);
        givenRepositoryReturnsTopOfPool();

        SectionedSearchHits result = service.search(QUERY, null);

        assertThat(result.hits(ResourceSection.BLOG)).hasSize(20);
        verify(embeddingClient, times(1)).embed(QUERY);
        verify(resourceSearchRepository, times(1)).findCandidates(vector, MODEL, null, 400, List.of());
    }

    @Test
    @DisplayName("블로그 섹션이 찬 범위까지만 쓰고 그 뒤 순위의 조각은 다른 섹션에도 담지 않는다")
    void usesOnlyCandidatesWithinTheRangeWhereBlogSectionBecameFull() {
        addMany("github-pr", 55, 0.9);
        addMany("blog-post", 20, 0.5);
        addMany("youtube-tekotalk", 45, 0.4);
        addMany("github-wiki", 30, 0.3);
        givenRepositoryReturnsTopOfPool();

        SectionedSearchHits result = service.search(QUERY, null);

        // 앞 120개(PR 55, 블로그 20, 테코톡 45)에서 블로그가 차므로 121번째부터인 위키는 문서 섹션에 들어가지 않는다.
        assertThat(result.hits(ResourceSection.BLOG)).hasSize(20);
        assertThat(result.hits(ResourceSection.DOCUMENT).size() + result.rest(ResourceSection.DOCUMENT).size())
                .isEqualTo(55);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20})
    @DisplayName("후보를 늘려 가며 여러 번 조회하던 방식과 섹션별 결과가 같다")
    void returnsSameSectionsAsQueryingOnceMorePerExpansion(long seed) {
        Random random = new Random(seed);
        String[] sources = {"github-pr", "github-pr", "github-pr", "github-pr", "github-teamdoc", "blog-post",
                "youtube-tekotalk", "unknown-source"};
        int documents = random.nextInt(450);
        for (int i = 0; i < documents; i++) {
            long documentId = add(sources[random.nextInt(sources.length)], random.nextDouble());
            for (int extra = random.nextInt(3); extra > 0; extra--) {
                addChunk(documentId, random.nextDouble());
            }
        }
        givenRepositoryReturnsTopOfPool();

        SectionedSearchHits result = service.search(QUERY, null);

        SectionedSearchHits expected = expandingOneQueryAtATime();
        for (ResourceSection section : ResourceSection.values()) {
            assertThat(result.hits(section)).isEqualTo(expected.hits(section));
            assertThat(result.rest(section)).isEqualTo(expected.rest(section));
        }
    }

    @Test
    @DisplayName("후보가 더 없으면 블로그가 모자라도 멈춘다")
    void stopsWhenCandidatesAreExhausted() {
        addMany("blog-post", 3, 0.9);
        addMany("github-pr", 10, 0.8);
        givenRepositoryReturnsTopOfPool();

        SectionedSearchHits result = service.search(QUERY, null);

        assertThat(result.hits(ResourceSection.BLOG)).hasSize(3);
        verify(resourceSearchRepository, times(1)).findCandidates(eq(vector), eq(MODEL), eq(null), anyInt(), any());
        assertThat(result.hits(ResourceSection.DOCUMENT)).hasSize(10);
    }

    @Test
    @DisplayName("후보는 400개까지만 보고, 블로그가 모자라도 그 뒤 순위는 읽지 않는다")
    void looksAtMostAtMaximumCandidates() {
        addMany("github-pr", 500, 0.9);
        addMany("blog-post", 20, 0.1);
        givenRepositoryReturnsTopOfPool();

        SectionedSearchHits result = service.search(QUERY, null);

        assertThat(result.hits(ResourceSection.BLOG)).isEmpty();
        verify(resourceSearchRepository, times(1)).findCandidates(eq(vector), eq(MODEL), eq(null), anyInt(), any());
        verify(resourceSearchRepository).findCandidates(vector, MODEL, null, 400, List.of());
    }

    @Test
    @DisplayName("섹션에 넣을 수 없는 원천의 자료는 결과에서 뺀다")
    void excludesUnknownSources() {
        add("unknown-source", 0.99);
        long blog = add("blog-post", 0.9);
        givenRepositoryReturnsTopOfPool();

        SectionedSearchHits result = service.search(QUERY, null);

        assertThat(ids(result, ResourceSection.BLOG)).containsExactly(blog);
        assertThat(result.hits(ResourceSection.DOCUMENT)).isEmpty();
        assertThat(result.hits(ResourceSection.TALK)).isEmpty();
    }

    @Test
    @DisplayName("검색 결과가 없으면 모든 섹션이 빈 목록이다")
    void returnsEmptySectionsWhenNothingMatches() {
        givenRepositoryReturnsTopOfPool();

        SectionedSearchHits result = service.search(QUERY, null);

        for (ResourceSection section : ResourceSection.values()) {
            assertThat(result.hits(section)).isEmpty();
        }
    }

    @Test
    @DisplayName("직군을 그대로 조회에 전달한다")
    void passesTrackToRepository() {
        givenEmbedding();
        when(lexicalChunkSearch.search(QUERY, ResourceTrack.BE, 400)).thenReturn(List.of(new LexicalMatch(7L, 1.0)));
        when(resourceSearchRepository.findCandidates(vector, MODEL, ResourceTrack.BE, 400, List.of(7L)))
                .thenReturn(new SearchCandidates(List.of(), List.of()));

        service.search(QUERY, ResourceTrack.BE);

        verify(lexicalChunkSearch).search(QUERY, ResourceTrack.BE, 400);
        verify(resourceSearchRepository).findCandidates(vector, MODEL, ResourceTrack.BE, 400, List.of(7L));
    }

    @Test
    @DisplayName("단어 검색으로만 찾은 자료도 섹션에 담고, 두 검색이 모두 찾은 자료를 앞에 둔다")
    void includesDocumentsFoundOnlyByLexicalSearch() {
        long semanticOnly = add("blog-post", 0.9);
        long both = add("blog-post", 0.8);
        long bothChunk = nextChunkId - 1;
        long lexicalOnlyDocument = nextId++;
        long lexicalOnlyChunk = nextChunkId++;
        lexicalOnly.add(new ResourceSearchCandidate(
                lexicalOnlyDocument, "key-" + lexicalOnlyDocument, null, null, "제목", "소개", "blog-post",
                ResourceTrack.BE, "https://example.com/" + lexicalOnlyDocument,
                new MatchedChunk(lexicalOnlyChunk, "intro", "조각", 0.1)));
        when(lexicalChunkSearch.search(QUERY, null, 400))
                .thenReturn(List.of(new LexicalMatch(bothChunk, 2.0), new LexicalMatch(lexicalOnlyChunk, 1.0)));
        givenRepositoryReturnsTopOfPool();

        SectionedSearchHits result = service.search(QUERY, null);

        assertThat(ids(result, ResourceSection.BLOG)).containsExactly(both, semanticOnly, lexicalOnlyDocument);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("빈 질의는 임베딩과 조회 전에 거절한다")
    void rejectsBlankQueryBeforeExternalCalls(String query) {
        assertThatThrownBy(() -> service.search(query, null))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(embeddingClient, resourceSearchRepository, lexicalChunkSearch);
    }

    @Test
    @DisplayName("섹션별 결과 목록은 밖에서 바꿀 수 없다")
    void exposesUnmodifiableSectionHits() {
        add("blog-post", 0.9);
        givenRepositoryReturnsTopOfPool();

        SectionedSearchHits result = service.search(QUERY, null);

        assertThat(result.hits(ResourceSection.BLOG)).isUnmodifiable();
        assertThat(result.hits(ResourceSection.TALK)).isUnmodifiable();
    }

    @Test
    @DisplayName("임베딩 호출이 실패하면 벡터 조회 없이 단어 검색 순위로 섹션을 채운다")
    void answersWithLexicalRankingWhenEmbeddingFails() {
        when(embeddingClient.embed(QUERY)).thenThrow(new EmbeddingException(503, "UPSTREAM_TIMEOUT", true));
        when(lexicalChunkSearch.searchResources(QUERY, ResourceTrack.BE, 400)).thenReturn(List.of(
                new RankedResource(3L, null, "github-pr"),
                new RankedResource(1L, 42L, "blog-post"),
                new RankedResource(2L, null, "blog-post"),
                new RankedResource(4L, null, "youtube-tekotalk")
        ));

        SectionedSearchHits result = service.search(QUERY, ResourceTrack.BE);

        assertThat(ids(result, ResourceSection.BLOG)).containsExactly(1L, 2L);
        assertThat(ids(result, ResourceSection.DOCUMENT)).containsExactly(3L);
        assertThat(ids(result, ResourceSection.TALK)).containsExactly(4L);
        assertThat(result.hits(ResourceSection.BLOG).getFirst().projectId()).isEqualTo(42L);
        verifyNoInteractions(resourceSearchRepository);
    }

    @ParameterizedTest
    @EnumSource(value = ResourceSearchErrorCode.class, names = {"RESOURCE_SEARCH_TIMEOUT", "RESOURCE_SEARCH_BUSY"})
    @DisplayName("벡터 조회가 시간 제한에 걸리거나 자리가 없으면 단어 검색 순위로 응답한다")
    void answersWithLexicalRankingWhenVectorSearchIsUnavailable(ResourceSearchErrorCode errorCode) {
        givenEmbedding();
        when(resourceSearchRepository.findCandidates(eq(vector), eq(MODEL), eq(null), anyInt(), any()))
                .thenThrow(new ServiceUnavailableException(errorCode));
        when(lexicalChunkSearch.searchResources(QUERY, null, 400))
                .thenReturn(List.of(new RankedResource(1L, null, "blog-post")));

        SectionedSearchHits result = service.search(QUERY, null);

        assertThat(ids(result, ResourceSection.BLOG)).containsExactly(1L);
    }

    @Test
    @DisplayName("단어 검색도 찾은 자료가 없으면 원래 실패를 그대로 알린다")
    void rethrowsOriginalFailureWhenLexicalSearchFindsNothing() {
        EmbeddingException embeddingFailure = new EmbeddingException(429, "RATE_LIMIT", true);
        when(embeddingClient.embed(QUERY)).thenThrow(embeddingFailure);
        when(lexicalChunkSearch.searchResources(QUERY, null, 400)).thenReturn(List.of());

        assertThatThrownBy(() -> service.search(QUERY, null)).isSameAs(embeddingFailure);
    }

    @Test
    @DisplayName("단어 검색도 찾은 자료가 없으면 벡터 조회의 지연 오류를 그대로 알린다")
    void rethrowsTimeoutWhenLexicalSearchFindsNothing() {
        givenEmbedding();
        ServiceUnavailableException timeout = new ServiceUnavailableException(ResourceSearchErrorCode.RESOURCE_SEARCH_TIMEOUT);
        when(resourceSearchRepository.findCandidates(eq(vector), eq(MODEL), eq(null), anyInt(), any())).thenThrow(timeout);

        assertThatThrownBy(() -> service.search(QUERY, null)).isSameAs(timeout);
    }

    @Test
    @DisplayName("검색 지연이나 혼잡이 아닌 서비스 오류는 단어 검색으로 대신하지 않는다")
    void doesNotFallBackOnOtherServiceErrors() {
        givenEmbedding();
        ServiceUnavailableException other = new ServiceUnavailableException(ResourceSearchErrorCode.RESOURCE_NOT_FOUND);
        when(resourceSearchRepository.findCandidates(eq(vector), eq(MODEL), eq(null), anyInt(), any())).thenThrow(other);

        assertThatThrownBy(() -> service.search(QUERY, null)).isSameAs(other);
        verify(lexicalChunkSearch, never()).searchResources(any(), any(), anyInt());
    }

    @Test
    @DisplayName("DB 장애는 단어 검색으로 대신하지 않는다. 카드도 DB에서 읽어야 해서 응답을 만들 수 없다")
    void doesNotFallBackOnDatabaseFailure() {
        givenEmbedding();
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("DB 연결 실패");
        when(resourceSearchRepository.findCandidates(eq(vector), eq(MODEL), eq(null), anyInt(), any())).thenThrow(failure);

        assertThatThrownBy(() -> service.search(QUERY, null)).isSameAs(failure);
        verify(lexicalChunkSearch, never()).searchResources(any(), any(), anyInt());
    }

    private void givenEmbedding() {
        when(embeddingClient.embed(QUERY)).thenReturn(vector);
        when(embeddingClient.modelId()).thenReturn(MODEL);
    }

    private void givenRepositoryReturnsTopOfPool() {
        givenEmbedding();
        when(resourceSearchRepository.findCandidates(eq(vector), eq(MODEL), eq(null), anyInt(), any()))
                .thenAnswer(invocation -> {
                    int limit = invocation.getArgument(3);
                    return new SearchCandidates(pool.subList(0, Math.min(limit, pool.size())), lexicalOnly);
                });
    }

    // 바꾸기 전 동작이다. 후보 수를 두 배씩 늘려 조회를 다시 하고, 블로그 섹션이 차거나 후보가 바닥나면 멈췄다.
    private SectionedSearchHits expandingOneQueryAtATime() {
        int candidateLimit = 60;
        while (true) {
            List<ResourceSearchCandidate> candidates = pool.subList(0, Math.min(candidateLimit, pool.size()));
            SectionedSearchHits hits = SectionedSearchHits.from(SearchHits.rank(candidates));
            if (hits.isFull(ResourceSection.BLOG) || candidates.size() < candidateLimit || candidateLimit == 400) {
                return hits;
            }
            candidateLimit = Math.min(400, candidateLimit * 2);
        }
    }

    private long add(String source, double score) {
        long id = nextId++;
        sources.put(id, source);
        addChunk(id, score);
        return id;
    }

    private void addChunk(long documentId, double score) {
        long chunkId = nextChunkId++;
        pool.add(new ResourceSearchCandidate(
                documentId, "key-" + documentId, null, null, "제목 " + documentId, "소개", sources.get(documentId),
                ResourceTrack.BE, "https://example.com/" + documentId,
                new MatchedChunk(chunkId, "intro", "조각", score)));
        pool.sort(Comparator.comparingDouble((ResourceSearchCandidate candidate) -> candidate.chunk().similarityScore())
                .reversed()
                .thenComparing(candidate -> candidate.chunk().chunkId()));
    }

    private void addMany(String source, int count, double score) {
        for (int i = 0; i < count; i++) {
            add(source, score - i * 0.0001);
        }
    }

    private List<Long> ids(SectionedSearchHits result, ResourceSection section) {
        return result.hits(section).stream().map(RankedResource::documentId).toList();
    }
}

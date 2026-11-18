package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.embedding.EmbeddingClient;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import com.shoutoutz.api.resourcesearch.application.dto.SectionedSearchHits;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SectionedResourceSearchServiceTest {

    private static final String MODEL = "model@1024";
    private static final String QUERY = "동시성";

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private ResourceSearchRepository resourceSearchRepository;

    @InjectMocks
    private SectionedResourceSearchService service;

    private float[] vector;
    private List<ResourceSearchCandidate> pool;
    private long nextId;

    @BeforeEach
    void setUp() {
        vector = new float[1024];
        vector[0] = 1;
        pool = new ArrayList<>();
        nextId = 1;
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
    @DisplayName("블로그 섹션이 차지 않으면 후보를 늘려 다시 찾고 질의는 한 번만 임베딩한다")
    void expandsCandidatesUntilBlogSectionIsFull() {
        addMany("github-pr", 100, 0.9);
        addMany("blog-post", 20, 0.5);
        givenRepositoryReturnsTopOfPool();

        SectionedSearchHits result = service.search(QUERY, null);

        assertThat(result.hits(ResourceSection.BLOG)).hasSize(20);
        verify(embeddingClient, times(1)).embed(QUERY);
        verify(resourceSearchRepository).findNearest(vector, MODEL, null, 60);
        verify(resourceSearchRepository).findNearest(vector, MODEL, null, 120);
    }

    @Test
    @DisplayName("후보가 더 없으면 블로그가 모자라도 멈춘다")
    void stopsWhenCandidatesAreExhausted() {
        addMany("blog-post", 3, 0.9);
        addMany("github-pr", 10, 0.8);
        givenRepositoryReturnsTopOfPool();

        SectionedSearchHits result = service.search(QUERY, null);

        assertThat(result.hits(ResourceSection.BLOG)).hasSize(3);
        verify(resourceSearchRepository, times(1)).findNearest(eq(vector), eq(MODEL), eq(null), anyInt());
    }

    @Test
    @DisplayName("후보 확대는 400개에서 멈춘다")
    void stopsExpandingAtMaximumCandidates() {
        addMany("github-pr", 500, 0.9);
        givenRepositoryReturnsTopOfPool();

        service.search(QUERY, null);

        verify(resourceSearchRepository).findNearest(vector, MODEL, null, 400);
        verify(resourceSearchRepository, times(4)).findNearest(eq(vector), eq(MODEL), eq(null), anyInt());
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
        when(resourceSearchRepository.findNearest(vector, MODEL, ResourceTrack.BE, 60)).thenReturn(List.of());

        service.search(QUERY, ResourceTrack.BE);

        verify(resourceSearchRepository).findNearest(vector, MODEL, ResourceTrack.BE, 60);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("빈 질의는 임베딩과 조회 전에 거절한다")
    void rejectsBlankQueryBeforeExternalCalls(String query) {
        assertThatThrownBy(() -> service.search(query, null))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(embeddingClient, resourceSearchRepository);
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

    private void givenEmbedding() {
        when(embeddingClient.embed(QUERY)).thenReturn(vector);
        when(embeddingClient.modelId()).thenReturn(MODEL);
    }

    private void givenRepositoryReturnsTopOfPool() {
        givenEmbedding();
        when(resourceSearchRepository.findNearest(eq(vector), eq(MODEL), eq(null), anyInt()))
                .thenAnswer(invocation -> {
                    int limit = invocation.getArgument(3);
                    return List.copyOf(pool.subList(0, Math.min(limit, pool.size())));
                });
    }

    private long add(String source, double score) {
        long id = nextId++;
        pool.add(new ResourceSearchCandidate(
                id, "key-" + id, null, null, "제목 " + id, "소개", source, ResourceTrack.BE,
                "https://example.com/" + id, new MatchedChunk(id * 10, "intro", "조각", score)));
        pool.sort((left, right) -> Double.compare(right.chunk().similarityScore(), left.chunk().similarityScore()));
        return id;
    }

    private void addMany(String source, int count, double score) {
        for (int i = 0; i < count; i++) {
            add(source, score - i * 0.0001);
        }
    }

    private List<Long> ids(SectionedSearchHits result, ResourceSection section) {
        return result.hits(section).stream().map(SearchHit::documentId).toList();
    }
}

package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.embedding.EmbeddingClient;
import com.shoutoutz.api.common.embedding.EmbeddingException;
import com.shoutoutz.api.resourcesearch.application.dto.HybridRanking;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class HybridResourceSearchServiceTest {

    private static final String MODEL = "model@1024";

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private HybridResourceRanker hybridResourceRanker;

    @InjectMocks
    private HybridResourceSearchService resourceSearchService;

    private float[] vector;

    @BeforeEach
    void setUp() {
        vector = new float[1024];
        vector[0] = 1;
    }

    @Test
    void 질의와_벡터_모델_트랙을_전달하고_결합_순위대로_반환한다() {
        givenEmbedding();
        when(hybridResourceRanker.rank("작업 유실 방지", vector, MODEL, ResourceTrack.BE, 20))
                .thenReturn(new HybridRanking(List.of(hit(2, 0.6), hit(1, 0.9)), true));

        List<SearchHit> hits = resourceSearchService.search("작업 유실 방지", ResourceTrack.BE, 10);

        verify(embeddingClient).embed("작업 유실 방지");
        assertThat(hits).extracting(SearchHit::documentId).containsExactly(2L, 1L);
        assertThat(hits).extracting(SearchHit::similarityScore).containsExactly(0.6, 0.9);
    }

    @Test
    void 요청한_문서수까지만_반환한다() {
        givenEmbedding();
        when(hybridResourceRanker.rank("질의", vector, MODEL, null, 20))
                .thenReturn(new HybridRanking(hits(5), false));

        assertThat(resourceSearchService.search("질의", null, 2)).extracting(SearchHit::documentId)
                .containsExactly(1L, 2L);
    }

    @Test
    void 문서가_부족하면_후보를_늘리고_질의는_한번만_임베딩한다() {
        givenEmbedding();
        when(hybridResourceRanker.rank("질의", vector, MODEL, null, 20)).thenReturn(new HybridRanking(hits(5), false));
        when(hybridResourceRanker.rank("질의", vector, MODEL, null, 40)).thenReturn(new HybridRanking(hits(10), false));

        List<SearchHit> hits = resourceSearchService.search("질의", null, 10);

        assertThat(hits).hasSize(10);
        verify(embeddingClient, times(1)).embed("질의");
        verify(hybridResourceRanker).rank("질의", vector, MODEL, null, 20);
        verify(hybridResourceRanker).rank("질의", vector, MODEL, null, 40);
    }

    @Test
    void 후보가_더_없으면_요청보다_적은_문서를_반환한다() {
        givenEmbedding();
        when(hybridResourceRanker.rank("질의", vector, MODEL, null, 20)).thenReturn(new HybridRanking(hits(1), true));

        assertThat(resourceSearchService.search("질의", null, 10)).hasSize(1);
        verify(hybridResourceRanker, times(1)).rank(any(), any(), any(), any(), anyInt());
    }

    @Test
    void 후보_확대는_400개에서_멈춘다() {
        givenEmbedding();
        when(hybridResourceRanker.rank(eq("질의"), eq(vector), eq(MODEL), eq(null), anyInt()))
                .thenAnswer(invocation -> new HybridRanking(hits(invocation.getArgument(4, Integer.class) / 4), false));

        assertThat(resourceSearchService.search("질의", null, 200)).hasSize(100);
        verify(hybridResourceRanker).rank("질의", vector, MODEL, null, 200);
        verify(hybridResourceRanker).rank("질의", vector, MODEL, null, 400);
        verify(hybridResourceRanker, times(2)).rank(any(), any(), any(), any(), anyInt());
    }

    @Test
    void 매우_큰_limit도_후보_상한을_넘기거나_정수_오버플로를_일으키지_않는다() {
        givenEmbedding();
        when(hybridResourceRanker.rank("질의", vector, MODEL, null, 400)).thenReturn(new HybridRanking(List.of(), true));

        assertThat(resourceSearchService.search("질의", null, Integer.MAX_VALUE)).isEmpty();
        verify(hybridResourceRanker).rank("질의", vector, MODEL, null, 400);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t"})
    void 빈_질의는_외부_호출_전에_거절한다(String query) {
        assertThatThrownBy(() -> resourceSearchService.search(query, null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(embeddingClient, hybridResourceRanker);
    }

    @ParameterizedTest
    @EnumSource(ResourceTrack.class)
    void 트랙_enum을_조회에_전달한다(ResourceTrack track) {
        givenEmbedding();
        when(hybridResourceRanker.rank("query", vector, MODEL, track, 20)).thenReturn(new HybridRanking(List.of(), true));

        assertThat(resourceSearchService.search("query", track, 1)).isEmpty();

        verify(hybridResourceRanker).rank("query", vector, MODEL, track, 20);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void 양수가_아닌_문서수는_외부_호출_전에_거절한다(int limit) {
        assertThatThrownBy(() -> resourceSearchService.search("질의", ResourceTrack.BE, limit))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(embeddingClient, hybridResourceRanker);
    }

    @Test
    void 임베딩_실패를_빈_결과로_바꾸거나_재시도하지_않는다() {
        EmbeddingException failure = new EmbeddingException(429, "RATE_LIMIT", true);
        when(embeddingClient.embed("질의")).thenThrow(failure);

        assertThatThrownBy(() -> resourceSearchService.search("질의", ResourceTrack.BE, 10)).isSameAs(failure);
        verifyNoInteractions(hybridResourceRanker);
        verify(embeddingClient, times(1)).embed("질의");
    }

    @Test
    void 조회_실패를_빈_결과로_숨기지_않는다() {
        givenEmbedding();
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("test failure");
        when(hybridResourceRanker.rank("질의", vector, MODEL, null, 20)).thenThrow(failure);

        assertThatThrownBy(() -> resourceSearchService.search("질의", null, 1)).isSameAs(failure);
    }

    private void givenEmbedding() {
        when(embeddingClient.embed(any())).thenReturn(vector);
        when(embeddingClient.modelId()).thenReturn(MODEL);
    }

    private List<SearchHit> hits(int documentCount) {
        List<SearchHit> hits = new ArrayList<>();
        for (int document = 1; document <= documentCount; document++) {
            hits.add(hit(document, 0.9));
        }
        return hits;
    }

    private SearchHit hit(long documentId, double similarityScore) {
        return new SearchHit(
                documentId,
                "repo#" + documentId,
                null,
                "example/repo",
                "작업 유실 방지",
                null,
                "github-pr",
                ResourceTrack.ALL,
                "https://example.com/" + documentId,
                similarityScore,
                List.of(new MatchedChunk(documentId * 10, "experience", "문제: 작업 유실\n해결: 재시도", similarityScore))
        );
    }
}

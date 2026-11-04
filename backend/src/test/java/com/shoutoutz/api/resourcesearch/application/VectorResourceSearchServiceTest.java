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
import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
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
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class VectorResourceSearchServiceTest {

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private ResourceSearchRepository resourceSearchRepository;

    @InjectMocks
    private VectorResourceSearchService resourceSearchService;

    private float[] vector;

    @BeforeEach
    void setUp() {
        vector = new float[1024];
        vector[0] = 1;
    }

    @Test
    void 질의와_모델_트랙을_전달하고_문서별_최고_점수와_조각을_반환한다() {
        givenEmbedding();
        when(resourceSearchRepository.findNearest(vector, "model@1024", ResourceTrack.BE, 20)).thenReturn(List.of(
                candidate(1, 11, "intro", 0.7),
                candidate(2, 21, "experience", 0.8),
                candidate(1, 12, "experience", 0.9)
        ));

        List<SearchHit> hits = resourceSearchService.search("작업 유실 방지", ResourceTrack.BE, 10);

        verify(embeddingClient).embed("작업 유실 방지");
        assertThat(hits).extracting(SearchHit::documentId).containsExactly(1L, 2L);
        assertThat(hits).extracting(SearchHit::similarityScore).containsExactly(0.9, 0.8);
        assertThat(hits.getFirst().matchedChunks()).extracting(MatchedChunk::chunkId).containsExactly(12L, 11L);
        assertThat(hits.getFirst().matchedChunks()).extracting(MatchedChunk::chunkKind).containsExactly("experience", "intro");
        assertThat(hits.getFirst().externalKey()).isEqualTo("repo#1");
        assertThat(hits.getFirst().repo()).isEqualTo("example/repo");
        assertThat(hits.getFirst().track()).isEqualTo(ResourceTrack.ALL);
        assertThat(JsonMapper.builder().build().valueToTree(hits.getFirst()).path("track").asString())
                .isEqualTo("ALL");
    }

    @Test
    void 문서수로_제한하고_동점은_문서ID와_조각ID로_정렬한다() {
        givenEmbedding();
        when(resourceSearchRepository.findNearest(vector, "model@1024", null, 20)).thenReturn(List.of(
                candidate(3, 31, "experience", 0.8),
                candidate(2, 22, "intro", 0.8),
                candidate(1, 12, "experience", 0.9),
                candidate(1, 11, "intro", 0.9)
        ));

        List<SearchHit> hits = resourceSearchService.search("질의", null, 2);

        assertThat(hits).extracting(SearchHit::documentId).containsExactly(1L, 2L);
        assertThat(hits.getFirst().matchedChunks()).extracting(MatchedChunk::chunkId).containsExactly(11L, 12L);
    }

    @Test
    void 문서가_부족하면_후보를_늘리고_질의는_한번만_임베딩한다() {
        givenEmbedding();
        when(resourceSearchRepository.findNearest(vector, "model@1024", null, 20)).thenReturn(candidates(5));
        when(resourceSearchRepository.findNearest(vector, "model@1024", null, 40)).thenReturn(candidates(10));

        List<SearchHit> hits = resourceSearchService.search("질의", null, 10);

        assertThat(hits).hasSize(10);
        verify(embeddingClient, times(1)).embed("질의");
        verify(resourceSearchRepository).findNearest(vector, "model@1024", null, 20);
        verify(resourceSearchRepository).findNearest(vector, "model@1024", null, 40);
    }

    @Test
    void 후보가_더_없으면_요청보다_적은_문서를_반환한다() {
        givenEmbedding();
        when(resourceSearchRepository.findNearest(vector, "model@1024", null, 20))
                .thenReturn(List.of(candidate(1, 1, "intro", 0.9)));

        assertThat(resourceSearchService.search("질의", null, 10)).hasSize(1);
        verify(resourceSearchRepository, times(1)).findNearest(any(), eq("model@1024"), eq(null), anyInt());
    }

    @Test
    void 후보_확대는_400개에서_멈춘다() {
        givenEmbedding();
        when(resourceSearchRepository.findNearest(any(), eq("model@1024"), eq(null), anyInt()))
                .thenAnswer(invocation -> candidates(invocation.getArgument(3, Integer.class) / 4));

        assertThat(resourceSearchService.search("질의", null, 200)).hasSize(100);
        verify(resourceSearchRepository).findNearest(vector, "model@1024", null, 200);
        verify(resourceSearchRepository).findNearest(vector, "model@1024", null, 400);
        verify(resourceSearchRepository, times(2)).findNearest(any(), eq("model@1024"), eq(null), anyInt());
    }

    @Test
    void 매우_큰_limit도_후보_상한을_넘기거나_정수_오버플로를_일으키지_않는다() {
        givenEmbedding();
        when(resourceSearchRepository.findNearest(vector, "model@1024", null, 400)).thenReturn(List.of());

        assertThat(resourceSearchService.search("질의", null, Integer.MAX_VALUE)).isEmpty();
        verify(resourceSearchRepository).findNearest(vector, "model@1024", null, 400);
    }

    @Test
    void 검색_결과가_없으면_빈_목록을_반환한다() {
        givenEmbedding();
        when(resourceSearchRepository.findNearest(vector, "model@1024", ResourceTrack.ALL, 20)).thenReturn(List.of());

        assertThat(resourceSearchService.search("질의", ResourceTrack.ALL, 5)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t"})
    void 빈_질의는_외부_호출_전에_거절한다(String query) {
        assertThatThrownBy(() -> resourceSearchService.search(query, null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(embeddingClient, resourceSearchRepository);
    }

    @ParameterizedTest
    @EnumSource(ResourceTrack.class)
    void 트랙_enum을_조회에_전달한다(ResourceTrack track) {
        givenEmbedding();
        when(resourceSearchRepository.findNearest(vector, "model@1024", track, 20)).thenReturn(List.of());

        assertThat(resourceSearchService.search("query", track, 1)).isEmpty();

        verify(resourceSearchRepository).findNearest(vector, "model@1024", track, 20);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void 양수가_아닌_문서수는_외부_호출_전에_거절한다(int limit) {
        assertThatThrownBy(() -> resourceSearchService.search("질의", ResourceTrack.BE, limit))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(embeddingClient, resourceSearchRepository);
    }

    @Test
    void 임베딩_실패를_빈_결과로_바꾸거나_재시도하지_않는다() {
        EmbeddingException failure = new EmbeddingException(429, "RATE_LIMIT", true);
        when(embeddingClient.embed("질의")).thenThrow(failure);

        assertThatThrownBy(() -> resourceSearchService.search("질의", ResourceTrack.BE, 10)).isSameAs(failure);
        verifyNoInteractions(resourceSearchRepository);
        verify(embeddingClient, times(1)).embed("질의");
    }

    @Test
    void DB_실패를_빈_결과로_숨기지_않는다() {
        givenEmbedding();
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("test failure");
        when(resourceSearchRepository.findNearest(vector, "model@1024", null, 20)).thenThrow(failure);

        assertThatThrownBy(() -> resourceSearchService.search("질의", null, 1)).isSameAs(failure);
    }

    private void givenEmbedding() {
        when(embeddingClient.embed(any())).thenReturn(vector);
        when(embeddingClient.modelId()).thenReturn("model@1024");
    }

    private List<ResourceSearchCandidate> candidates(int documentCount) {
        List<ResourceSearchCandidate> candidates = new ArrayList<>();
        for (int document = 1; document <= documentCount; document++) {
            for (int chunk = 0; chunk < 4; chunk++) {
                String kind = "experience";
                if (chunk == 0) {
                    kind = "intro";
                }
                candidates.add(candidate(document, document * 4L + chunk, kind, 0.9));
            }
        }
        return candidates;
    }

    private ResourceSearchCandidate candidate(long documentId, long chunkId, String kind, double score) {
        return new ResourceSearchCandidate(
                documentId,
                "repo#" + documentId,
                null,
                "example/repo",
                "작업 유실 방지",
                null,
                "github-pr",
                ResourceTrack.ALL,
                "https://example.com/" + documentId,
                new MatchedChunk(chunkId, kind, "문제: 작업 유실\n해결: 재시도", score)
        );
    }
}

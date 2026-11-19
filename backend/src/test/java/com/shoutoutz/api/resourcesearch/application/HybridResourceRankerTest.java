package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.resourcesearch.application.dto.LexicalMatch;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.application.dto.SearchCandidates;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class HybridResourceRankerTest {

    private static final String MODEL = "model@1024";
    private static final String QUERY = "분산 락";

    @Mock
    private ResourceSearchRepository resourceSearchRepository;

    @Mock
    private LexicalChunkSearch lexicalChunkSearch;

    @InjectMocks
    private HybridResourceRanker ranker;

    private final float[] vector = new float[1024];

    @Test
    @DisplayName("렉시컬 검색을 먼저 하고, 찾은 조각 ID를 벡터 조회에 함께 넘겨 DB를 한 번만 다녀온다")
    void 렉시컬_조각을_벡터_조회와_함께_읽는다() {
        when(lexicalChunkSearch.search(QUERY, ResourceTrack.BE, 400)).thenReturn(List.of(match(31), match(11)));
        when(resourceSearchRepository.findCandidates(vector, MODEL, ResourceTrack.BE, 400, List.of(31L, 11L)))
                .thenReturn(new SearchCandidates(List.of(candidate(1, 11, 0.9)), List.of(candidate(3, 31, 0.3))));

        HybridCandidates candidates = ranker.collect(QUERY, vector, MODEL, ResourceTrack.BE, 400);

        InOrder order = inOrder(lexicalChunkSearch, resourceSearchRepository);
        order.verify(lexicalChunkSearch).search(QUERY, ResourceTrack.BE, 400);
        order.verify(resourceSearchRepository).findCandidates(vector, MODEL, ResourceTrack.BE, 400, List.of(31L, 11L));
        order.verifyNoMoreInteractions();
        assertThat(candidates.rank(400).hits()).extracting(SearchHit::documentId).containsExactly(1L, 3L);
    }

    @Test
    @DisplayName("DB 실패를 빈 결과로 숨기지 않는다")
    void DB_실패를_숨기지_않는다() {
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("test failure");
        when(lexicalChunkSearch.search(any(), any(), anyInt())).thenReturn(List.of());
        when(resourceSearchRepository.findCandidates(any(), any(), any(), anyInt(), any())).thenThrow(failure);

        assertThatThrownBy(() -> ranker.collect(QUERY, vector, MODEL, null, 400)).isSameAs(failure);
    }

    private static LexicalMatch match(long chunkId) {
        return new LexicalMatch(chunkId, 1.0);
    }

    private static ResourceSearchCandidate candidate(long documentId, long chunkId, double similarityScore) {
        return new ResourceSearchCandidate(
                documentId,
                "repo#" + documentId,
                null,
                "example/repo",
                "title " + documentId,
                "summary",
                "github-pr",
                ResourceTrack.ALL,
                "https://example.com/" + documentId,
                new MatchedChunk(chunkId, "experience", "text", similarityScore)
        );
    }
}

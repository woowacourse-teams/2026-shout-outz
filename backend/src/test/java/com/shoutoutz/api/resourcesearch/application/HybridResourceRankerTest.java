package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.resourcesearch.application.dto.HybridRanking;
import com.shoutoutz.api.resourcesearch.application.dto.LexicalMatch;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private float[] vector;

    @BeforeEach
    void setUp() {
        vector = new float[1024];
        vector[0] = 1;
    }

    @Test
    @DisplayName("두 검색에서 모두 상위인 문서가 한쪽에서만 1위인 문서보다 앞선다")
    void 두_검색에서_모두_상위인_문서가_앞선다() {
        givenSemantic(20, candidate(1, 11, 0.9), candidate(2, 21, 0.8));
        givenLexical(20, match(31), match(21));
        givenChunks(List.of(31L), candidate(3, 31, 0.3));

        HybridRanking ranking = ranker.rank(QUERY, vector, MODEL, null, 20);

        assertThat(ranking.hits()).extracting(SearchHit::documentId).containsExactly(2L, 1L, 3L);
    }

    @Test
    @DisplayName("렉시컬로만 찾은 조각만 따로 조회하고, 그 문서의 유사도는 조회한 코사인 값을 쓴다")
    void 렉시컬로만_찾은_조각만_조회한다() {
        givenSemantic(20, candidate(1, 11, 0.9));
        givenLexical(20, match(11), match(41));
        givenChunks(List.of(41L), candidate(4, 41, 0.2));

        HybridRanking ranking = ranker.rank(QUERY, vector, MODEL, null, 20);

        verify(resourceSearchRepository).findByChunkIds(List.of(41L), vector, MODEL);
        assertThat(ranking.hits()).extracting(SearchHit::documentId).containsExactly(1L, 4L);
        assertThat(ranking.hits().get(1).similarityScore()).isEqualTo(0.2);
    }

    @Test
    @DisplayName("한 문서의 조각이 두 검색에 나뉘어 나오면 문서 하나로 합치고 조각은 유사도 순으로 둔다")
    void 같은_문서의_조각을_합친다() {
        givenSemantic(20, candidate(1, 11, 0.9));
        givenLexical(20, match(12));
        givenChunks(List.of(12L), candidate(1, 12, 0.4));

        HybridRanking ranking = ranker.rank(QUERY, vector, MODEL, null, 20);

        assertThat(ranking.hits()).hasSize(1);
        SearchHit hit = ranking.hits().getFirst();
        assertThat(hit.similarityScore()).isEqualTo(0.9);
        assertThat(hit.matchedChunks()).extracting(MatchedChunk::chunkId).containsExactly(11L, 12L);
    }

    @Test
    @DisplayName("색인을 만든 뒤 지워진 조각은 렉시컬 순위에서 빠진다")
    void 조회되지_않는_조각은_순위에서_뺀다() {
        givenSemantic(20, candidate(1, 11, 0.9));
        givenLexical(20, match(51), match(61));
        givenChunks(List.of(51L, 61L), candidate(6, 61, 0.1));

        HybridRanking ranking = ranker.rank(QUERY, vector, MODEL, null, 20);

        assertThat(ranking.hits()).extracting(SearchHit::documentId).containsExactly(1L, 6L);
    }

    @Test
    @DisplayName("렉시컬 결과가 없으면 시맨틱 순위를 그대로 따른다")
    void 렉시컬_결과가_없으면_시맨틱_순위다() {
        givenSemantic(20, candidate(2, 21, 0.7), candidate(1, 11, 0.9));
        givenLexical(20);
        givenChunks(List.of());

        HybridRanking ranking = ranker.rank(QUERY, vector, MODEL, null, 20);

        assertThat(ranking.hits()).extracting(SearchHit::documentId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("시맨틱 결과가 없으면 렉시컬 순위를 그대로 따른다")
    void 시맨틱_결과가_없으면_렉시컬_순위다() {
        givenSemantic(20);
        givenLexical(20, match(21), match(11));
        givenChunks(List.of(21L, 11L), candidate(1, 11, 0.5), candidate(2, 21, 0.1));

        HybridRanking ranking = ranker.rank(QUERY, vector, MODEL, null, 20);

        assertThat(ranking.hits()).extracting(SearchHit::documentId).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("트랙과 후보 수를 두 검색에 똑같이 전달한다")
    void 트랙과_후보수를_두_검색에_전달한다() {
        when(resourceSearchRepository.findNearest(vector, MODEL, ResourceTrack.BE, 60)).thenReturn(List.of());
        when(lexicalChunkSearch.search(QUERY, ResourceTrack.BE, 60)).thenReturn(List.of());
        givenChunks(List.of());

        ranker.rank(QUERY, vector, MODEL, ResourceTrack.BE, 60);

        verify(resourceSearchRepository).findNearest(vector, MODEL, ResourceTrack.BE, 60);
        verify(lexicalChunkSearch).search(QUERY, ResourceTrack.BE, 60);
    }

    @Test
    @DisplayName("한쪽이라도 후보를 요청한 만큼 채웠으면 더 찾을 자료가 남은 것으로 본다")
    void 한쪽이라도_후보를_채우면_소진이_아니다() {
        givenSemantic(2, candidate(1, 11, 0.9));
        givenLexical(2, match(11), match(12));
        givenChunks(List.of(12L), candidate(1, 12, 0.4));

        assertThat(ranker.rank(QUERY, vector, MODEL, null, 2).exhausted()).isFalse();
    }

    @Test
    @DisplayName("두 검색 모두 후보가 요청보다 적으면 더 찾을 자료가 없다")
    void 두_검색_모두_후보가_모자라면_소진이다() {
        givenSemantic(2, candidate(1, 11, 0.9));
        givenLexical(2, match(11));
        givenChunks(List.of());

        assertThat(ranker.rank(QUERY, vector, MODEL, null, 2).exhausted()).isTrue();
    }

    @Test
    @DisplayName("DB 실패를 빈 결과로 숨기지 않는다")
    void DB_실패를_숨기지_않는다() {
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("test failure");
        when(resourceSearchRepository.findNearest(any(), any(), any(), anyInt())).thenThrow(failure);

        assertThatThrownBy(() -> ranker.rank(QUERY, vector, MODEL, null, 20)).isSameAs(failure);
    }

    private void givenSemantic(int candidateLimit, ResourceSearchCandidate... candidates) {
        when(resourceSearchRepository.findNearest(vector, MODEL, null, candidateLimit)).thenReturn(List.of(candidates));
    }

    private void givenLexical(int candidateLimit, LexicalMatch... matches) {
        when(lexicalChunkSearch.search(QUERY, null, candidateLimit)).thenReturn(List.of(matches));
    }

    private void givenChunks(List<Long> chunkIds, ResourceSearchCandidate... candidates) {
        when(resourceSearchRepository.findByChunkIds(new ArrayList<>(chunkIds), vector, MODEL))
                .thenReturn(List.of(candidates));
    }

    private LexicalMatch match(long chunkId) {
        return new LexicalMatch(chunkId, 1.0);
    }

    private ResourceSearchCandidate candidate(long documentId, long chunkId, double similarityScore) {
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

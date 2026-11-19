package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcesearch.application.dto.HybridRanking;
import com.shoutoutz.api.resourcesearch.application.dto.LexicalMatch;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.application.dto.SearchCandidates;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HybridCandidatesTest {

    @Test
    @DisplayName("두 검색에서 모두 상위인 문서가 한쪽에서만 1위인 문서보다 앞선다")
    void 두_검색에서_모두_상위인_문서가_앞선다() {
        HybridCandidates candidates = candidates(
                List.of(candidate(1, 11, 0.9), candidate(2, 21, 0.8)),
                List.of(match(31), match(21)),
                List.of(candidate(3, 31, 0.3))
        );

        assertThat(candidates.rank(20).hits()).extracting(SearchHit::documentId).containsExactly(2L, 1L, 3L);
    }

    @Test
    @DisplayName("렉시컬로만 찾은 문서의 유사도는 함께 읽어 온 값을 쓴다")
    void 렉시컬로만_찾은_문서의_유사도() {
        HybridCandidates candidates = candidates(
                List.of(candidate(1, 11, 0.9)),
                List.of(match(11), match(41)),
                List.of(candidate(4, 41, 0.2))
        );

        HybridRanking ranking = candidates.rank(20);

        assertThat(ranking.hits()).extracting(SearchHit::documentId).containsExactly(1L, 4L);
        assertThat(ranking.hits().get(1).similarityScore()).isEqualTo(0.2);
    }

    @Test
    @DisplayName("한 문서의 조각이 두 검색에 나뉘어 나오면 문서 하나로 합치고 조각은 유사도 순으로 둔다")
    void 같은_문서의_조각을_합친다() {
        HybridCandidates candidates = candidates(
                List.of(candidate(1, 11, 0.9)),
                List.of(match(12)),
                List.of(candidate(1, 12, 0.4))
        );

        HybridRanking ranking = candidates.rank(20);

        assertThat(ranking.hits()).hasSize(1);
        SearchHit hit = ranking.hits().getFirst();
        assertThat(hit.similarityScore()).isEqualTo(0.9);
        assertThat(hit.matchedChunks()).extracting(MatchedChunk::chunkId).containsExactly(11L, 12L);
    }

    @Test
    @DisplayName("색인을 만든 뒤 지워져 조회되지 않은 조각은 렉시컬 순위에서 빠진다")
    void 조회되지_않는_조각은_순위에서_뺀다() {
        HybridCandidates candidates = candidates(
                List.of(candidate(1, 11, 0.9)),
                List.of(match(51), match(61)),
                List.of(candidate(6, 61, 0.1))
        );

        assertThat(candidates.rank(20).hits()).extracting(SearchHit::documentId).containsExactly(1L, 6L);
    }

    @Test
    @DisplayName("렉시컬 결과가 없으면 시맨틱 순위를 그대로 따른다")
    void 렉시컬_결과가_없으면_시맨틱_순위다() {
        HybridCandidates candidates = candidates(
                List.of(candidate(1, 11, 0.9), candidate(2, 21, 0.7)), List.of(), List.of()
        );

        assertThat(candidates.rank(20).hits()).extracting(SearchHit::documentId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("시맨틱 결과가 없으면 렉시컬 순위를 그대로 따른다")
    void 시맨틱_결과가_없으면_렉시컬_순위다() {
        HybridCandidates candidates = candidates(
                List.of(),
                List.of(match(21), match(11)),
                List.of(candidate(1, 11, 0.5), candidate(2, 21, 0.1))
        );

        assertThat(candidates.rank(20).hits()).extracting(SearchHit::documentId).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("앞에서부터 자른 범위만 본다. 범위 밖 시맨틱 조각은 렉시컬이 찾았을 때만 들어온다")
    void 범위_밖의_후보는_보지_않는다() {
        HybridCandidates candidates = candidates(
                List.of(candidate(1, 11, 0.9), candidate(2, 21, 0.8), candidate(3, 31, 0.7)),
                List.of(match(31), match(41)),
                List.of(candidate(4, 41, 0.1))
        );

        assertThat(candidates.rank(1).hits()).extracting(SearchHit::documentId).containsExactly(1L, 3L);
        assertThat(candidates.rank(2).hits()).extracting(SearchHit::documentId).containsExactly(1L, 3L, 2L, 4L);
    }

    @Test
    @DisplayName("범위를 넓힌 결과는 같은 후보로 다시 불러도 같다")
    void 같은_범위는_같은_결과다() {
        HybridCandidates candidates = candidates(
                List.of(candidate(1, 11, 0.9), candidate(2, 21, 0.8)),
                List.of(match(21), match(11)),
                List.of()
        );

        assertThat(candidates.rank(2).hits()).isEqualTo(candidates.rank(2).hits());
    }

    @Test
    @DisplayName("한쪽이라도 후보를 요청한 만큼 채웠으면 더 찾을 자료가 남은 것으로 본다")
    void 한쪽이라도_후보를_채우면_소진이_아니다() {
        HybridCandidates candidates = candidates(
                List.of(candidate(1, 11, 0.9)),
                List.of(match(11), match(12)),
                List.of(candidate(1, 12, 0.4))
        );

        assertThat(candidates.rank(2).exhausted()).isFalse();
    }

    @Test
    @DisplayName("두 검색 모두 후보가 요청보다 적으면 더 찾을 자료가 없다")
    void 두_검색_모두_후보가_모자라면_소진이다() {
        HybridCandidates candidates = candidates(List.of(candidate(1, 11, 0.9)), List.of(match(11)), List.of());

        assertThat(candidates.rank(2).exhausted()).isTrue();
    }

    private static HybridCandidates candidates(
            List<ResourceSearchCandidate> nearest,
            List<LexicalMatch> lexical,
            List<ResourceSearchCandidate> lexicalOnly
    ) {
        return new HybridCandidates(new SearchCandidates(nearest, lexicalOnly), lexical);
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

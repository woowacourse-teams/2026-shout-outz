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
import com.shoutoutz.api.resourcesearch.application.dto.LexicalMatch;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.application.dto.SearchCandidates;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class HybridResourceSearchServiceTest {

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private ResourceSearchRepository resourceSearchRepository;

    @Mock
    private LexicalChunkSearch lexicalChunkSearch;

    private HybridResourceSearchService resourceSearchService;

    private float[] vector;

    @BeforeEach
    void setUp() {
        vector = new float[1024];
        vector[0] = 1;
        resourceSearchService = new HybridResourceSearchService(
                embeddingClient, new HybridResourceRanker(resourceSearchRepository, lexicalChunkSearch)
        );
    }

    @Test
    void 질의와_모델_트랙을_전달하고_문서별_최고_점수와_조각을_반환한다() {
        givenEmbedding();
        when(resourceSearchRepository.findCandidates(vector, "model@1024", ResourceTrack.BE, 400, List.of())).thenReturn(nearestOnly(List.of(
                candidate(1, 11, "intro", 0.7),
                candidate(2, 21, "experience", 0.8),
                candidate(1, 12, "experience", 0.9)
        )));

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
        when(resourceSearchRepository.findCandidates(vector, "model@1024", null, 400, List.of())).thenReturn(nearestOnly(List.of(
                candidate(3, 31, "experience", 0.8),
                candidate(2, 22, "intro", 0.8),
                candidate(1, 12, "experience", 0.9),
                candidate(1, 11, "intro", 0.9)
        )));

        List<SearchHit> hits = resourceSearchService.search("질의", null, 2);

        assertThat(hits).extracting(SearchHit::documentId).containsExactly(1L, 2L);
        assertThat(hits.getFirst().matchedChunks()).extracting(MatchedChunk::chunkId).containsExactly(11L, 12L);
    }

    @Test
    void 문서가_부족하면_후보_범위를_넓히되_조회와_임베딩은_한번만_한다() {
        givenEmbedding();
        givenNearest(candidates(10));

        List<SearchHit> hits = resourceSearchService.search("질의", null, 10);

        assertThat(hits).hasSize(10);
        verify(embeddingClient, times(1)).embed("질의");
        verify(resourceSearchRepository, times(1)).findCandidates(vector, "model@1024", null, 400, List.of());
    }

    @Test
    void 문서수가_찬_범위까지만_쓰고_그_뒤_순위의_조각은_담지_않는다() {
        givenEmbedding();
        List<ResourceSearchCandidate> nearest = new ArrayList<>();
        nearest.add(candidate(1, 11, "intro", 0.99));
        for (int document = 2; document <= 30; document++) {
            nearest.add(candidate(document, document * 10L, "intro", 0.9));
        }
        nearest.add(candidate(1, 12, "experience", 0.5));
        givenNearest(nearest);

        List<SearchHit> hits = resourceSearchService.search("질의", null, 1);

        // 앞 20개에서 문서 1건이 차므로 31번째인 문서 1의 두 번째 조각은 근거 조각에 들어가지 않는다.
        assertThat(hits).extracting(SearchHit::documentId).containsExactly(1L);
        assertThat(hits.getFirst().matchedChunks()).extracting(MatchedChunk::chunkId).containsExactly(11L);
    }

    @Test
    void 후보가_더_없으면_요청보다_적은_문서를_반환한다() {
        givenEmbedding();
        givenNearest(List.of(candidate(1, 1, "intro", 0.9)));

        assertThat(resourceSearchService.search("질의", null, 10)).hasSize(1);
        verify(resourceSearchRepository, times(1)).findCandidates(any(), eq("model@1024"), eq(null), anyInt(), any());
    }

    @Test
    void 후보는_400개까지만_보고_문서가_모자라도_다시_조회하지_않는다() {
        givenEmbedding();
        givenNearest(candidates(100));

        assertThat(resourceSearchService.search("질의", null, 200)).hasSize(100);
        verify(resourceSearchRepository, times(1)).findCandidates(any(), eq("model@1024"), eq(null), anyInt(), any());
        verify(resourceSearchRepository).findCandidates(vector, "model@1024", null, 400, List.of());
    }

    @ParameterizedTest
    @CsvSource({"1, 1", "2, 5", "3, 10", "4, 20", "5, 21", "6, 40", "7, 100", "8, 200", "9, 400", "10, 10", "11, 3",
            "12, 60"})
    void 후보를_늘려_가며_여러_번_조회하던_방식과_결과가_같다(long seed, int limit) {
        givenEmbedding();
        Random random = new Random(seed);
        List<ResourceSearchCandidate> pool = new ArrayList<>();
        int chunks = random.nextInt(700);
        for (int chunk = 1; chunk <= chunks; chunk++) {
            pool.add(candidate(1 + random.nextInt(1 + chunks / 3), chunk, "intro", random.nextDouble()));
        }
        pool.sort(Comparator.comparingDouble((ResourceSearchCandidate candidate) -> candidate.chunk().similarityScore())
                .reversed()
                .thenComparing(candidate -> candidate.chunk().chunkId()));
        givenNearest(pool.subList(0, Math.min(400, pool.size())));

        List<SearchHit> hits = resourceSearchService.search("질의", null, limit);

        assertThat(hits).isEqualTo(expandingOneQueryAtATime(pool, limit));
    }

    @Test
    void 단어_검색으로만_찾은_자료도_담고_두_검색이_모두_찾은_자료를_앞에_둔다() {
        givenEmbedding();
        when(lexicalChunkSearch.search("질의", null, 400))
                .thenReturn(List.of(new LexicalMatch(21L, 2.0), new LexicalMatch(31L, 1.0)));
        when(resourceSearchRepository.findCandidates(vector, "model@1024", null, 400, List.of(21L, 31L)))
                .thenReturn(new SearchCandidates(
                        List.of(candidate(1, 11, "intro", 0.9), candidate(2, 21, "intro", 0.8)),
                        List.of(candidate(3, 31, "intro", 0.1))
                ));

        List<SearchHit> hits = resourceSearchService.search("질의", null, 10);

        assertThat(hits).extracting(SearchHit::documentId).containsExactly(2L, 1L, 3L);
    }

    @Test
    void 매우_큰_limit도_후보_상한을_넘기거나_정수_오버플로를_일으키지_않는다() {
        givenEmbedding();
        when(resourceSearchRepository.findCandidates(vector, "model@1024", null, 400, List.of())).thenReturn(noCandidates());

        assertThat(resourceSearchService.search("질의", null, Integer.MAX_VALUE)).isEmpty();
        verify(resourceSearchRepository).findCandidates(vector, "model@1024", null, 400, List.of());
    }

    @Test
    void 검색_결과가_없으면_빈_목록을_반환한다() {
        givenEmbedding();
        when(resourceSearchRepository.findCandidates(vector, "model@1024", ResourceTrack.ALL, 400, List.of())).thenReturn(noCandidates());

        assertThat(resourceSearchService.search("질의", ResourceTrack.ALL, 5)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t"})
    void 빈_질의는_외부_호출_전에_거절한다(String query) {
        assertThatThrownBy(() -> resourceSearchService.search(query, null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(embeddingClient, resourceSearchRepository, lexicalChunkSearch);
    }

    @ParameterizedTest
    @EnumSource(ResourceTrack.class)
    void 트랙_enum을_조회에_전달한다(ResourceTrack track) {
        givenEmbedding();
        when(resourceSearchRepository.findCandidates(vector, "model@1024", track, 400, List.of())).thenReturn(noCandidates());

        assertThat(resourceSearchService.search("query", track, 1)).isEmpty();

        verify(resourceSearchRepository).findCandidates(vector, "model@1024", track, 400, List.of());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void 양수가_아닌_문서수는_외부_호출_전에_거절한다(int limit) {
        assertThatThrownBy(() -> resourceSearchService.search("질의", ResourceTrack.BE, limit))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(embeddingClient, resourceSearchRepository, lexicalChunkSearch);
    }

    @Test
    void 임베딩_실패를_빈_결과로_바꾸거나_재시도하지_않는다() {
        EmbeddingException failure = new EmbeddingException(429, "RATE_LIMIT", true);
        when(embeddingClient.embed("질의")).thenThrow(failure);

        assertThatThrownBy(() -> resourceSearchService.search("질의", ResourceTrack.BE, 10)).isSameAs(failure);
        verifyNoInteractions(resourceSearchRepository, lexicalChunkSearch);
        verify(embeddingClient, times(1)).embed("질의");
    }

    @Test
    void DB_실패를_빈_결과로_숨기지_않는다() {
        givenEmbedding();
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("test failure");
        when(resourceSearchRepository.findCandidates(vector, "model@1024", null, 400, List.of())).thenThrow(failure);

        assertThatThrownBy(() -> resourceSearchService.search("질의", null, 1)).isSameAs(failure);
    }

    private void givenNearest(List<ResourceSearchCandidate> nearest) {
        when(resourceSearchRepository.findCandidates(vector, "model@1024", null, 400, List.of())).thenReturn(nearestOnly(nearest));
    }

    // 바꾸기 전 동작이다. 후보 수를 두 배씩 늘려 조회를 다시 하고, 문서 수가 차거나 후보가 바닥나면 멈췄다.
    private List<SearchHit> expandingOneQueryAtATime(List<ResourceSearchCandidate> pool, int limit) {
        int candidateLimit = Math.min(400, Math.max(20, limit));
        while (true) {
            List<ResourceSearchCandidate> candidates = pool.subList(0, Math.min(candidateLimit, pool.size()));
            List<SearchHit> hits = SearchHits.rank(candidates).stream().limit(limit).toList();
            if (hits.size() >= limit || candidates.size() < candidateLimit || candidateLimit == 400) {
                return hits;
            }
            candidateLimit = Math.min(400, candidateLimit * 2);
        }
    }

    private static SearchCandidates nearestOnly(List<ResourceSearchCandidate> nearest) {
        return new SearchCandidates(nearest, List.of());
    }

    private static SearchCandidates noCandidates() {
        return new SearchCandidates(List.of(), List.of());
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

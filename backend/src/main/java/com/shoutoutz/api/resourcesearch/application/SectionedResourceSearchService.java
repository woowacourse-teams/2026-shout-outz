package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.common.embedding.EmbeddingClient;
import com.shoutoutz.api.common.embedding.EmbeddingException;
import com.shoutoutz.api.common.exception.code.ErrorCode;
import com.shoutoutz.api.common.exception.custom.ServiceUnavailableException;
import com.shoutoutz.api.resourcesearch.application.dto.HybridRanking;
import com.shoutoutz.api.resourcesearch.application.dto.RankedResource;
import com.shoutoutz.api.resourcesearch.application.dto.SectionedSearchHits;
import com.shoutoutz.api.resourcesearch.domain.ResourceSearchErrorCode;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectionedResourceSearchService {

    private static final int INITIAL_CANDIDATES = 60;
    private static final int MAX_CANDIDATES = 400;
    private static final ResourceSection PRIORITY_SECTION = ResourceSection.BLOG;
    // 벡터 조회가 시간 제한에 걸렸거나 동시 실행 자리를 얻지 못했을 때의 오류다.
    private static final Set<ErrorCode> VECTOR_SEARCH_UNAVAILABLE = Set.of(
            ResourceSearchErrorCode.RESOURCE_SEARCH_TIMEOUT,
            ResourceSearchErrorCode.RESOURCE_SEARCH_BUSY
    );

    private final EmbeddingClient embeddingClient;
    private final HybridResourceRanker hybridResourceRanker;
    private final LexicalChunkSearch lexicalChunkSearch;

    /**
     * 임베딩 호출이 실패하거나 벡터 조회가 지연, 혼잡으로 막히면 단어 검색 순위만으로 응답한다.
     * 단어 검색은 서버 메모리의 색인에서 돌아 임베딩도 벡터 조회도 필요 없다. 오류 화면보다 덜 정확한 결과가 낫다고 본다.
     * 단어 검색도 찾은 자료가 없으면 빈 결과로 숨기지 않고 원래 실패를 알린다.
     * DB 장애는 대신하지 않는다. 카드도 DB에서 읽어야 해서 응답을 만들 수 없다.
     */
    public SectionedSearchHits search(String query, ResourceTrack track) {
        validateQuery(query);
        try {
            return searchHybrid(query, track);
        } catch (EmbeddingException exception) {
            return searchLexicalOnly(query, track, exception, "EMBEDDING_" + exception.errorCode());
        } catch (ServiceUnavailableException exception) {
            if (!VECTOR_SEARCH_UNAVAILABLE.contains(exception.getErrorCode())) {
                throw exception;
            }
            return searchLexicalOnly(query, track, exception, exception.getErrorCode().name());
        }
    }

    /**
     * 블로그는 전체 자료의 일부라 가까운 조각 몇십 개 안에 20건이 들어오지 않는 일이 흔하다.
     * 그래서 블로그 섹션이 찰 때까지 보는 범위를 60, 120, 240, 400개로 넓힌다.
     * 범위를 넓힐 때마다 조회를 다시 하지 않고, 벡터와 단어 검색 후보를 가장 넓은 범위로 한 번 모아 앞에서부터 잘라 본다.
     */
    private SectionedSearchHits searchHybrid(String query, ResourceTrack track) {
        float[] queryVector = embeddingClient.embed(query);
        String modelId = embeddingClient.modelId();
        HybridCandidates candidates = hybridResourceRanker.collect(query, queryVector, modelId, track, MAX_CANDIDATES);

        int candidateLimit = INITIAL_CANDIDATES;
        while (true) {
            HybridRanking ranking = candidates.rank(candidateLimit);
            SectionedSearchHits hits = SectionedSearchHits.from(ranking.hits());
            if (hits.isFull(PRIORITY_SECTION) || ranking.exhausted() || candidateLimit == MAX_CANDIDATES) {
                return hits;
            }
            candidateLimit = Math.min(MAX_CANDIDATES, candidateLimit * 2);
        }
    }

    private SectionedSearchHits searchLexicalOnly(
            String query,
            ResourceTrack track,
            RuntimeException failure,
            String reason
    ) {
        List<RankedResource> resources = lexicalChunkSearch.searchResources(query, track, MAX_CANDIDATES);
        if (resources.isEmpty()) {
            throw failure;
        }
        log.warn("Resource search answered with lexical ranking only: reason={}, documents={}", reason, resources.size());
        return SectionedSearchHits.fromRanked(resources);
    }

    private void validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("검색어는 비어 있을 수 없습니다.");
        }
    }
}

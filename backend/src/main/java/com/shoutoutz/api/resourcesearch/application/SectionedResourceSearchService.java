package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.common.embedding.EmbeddingClient;
import com.shoutoutz.api.resourcesearch.application.dto.HybridRanking;
import com.shoutoutz.api.resourcesearch.application.dto.SectionedSearchHits;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SectionedResourceSearchService {

    private static final int INITIAL_CANDIDATES = 60;
    private static final int MAX_CANDIDATES = 400;
    private static final ResourceSection PRIORITY_SECTION = ResourceSection.BLOG;

    private final EmbeddingClient embeddingClient;
    private final HybridResourceRanker hybridResourceRanker;

    /**
     * 블로그는 전체 자료의 일부라 가까운 조각 몇십 개 안에 20건이 들어오지 않는 일이 흔하다.
     * 그래서 블로그 섹션이 찰 때까지 보는 범위를 60, 120, 240, 400개로 넓힌다.
     * 범위를 넓힐 때마다 조회를 다시 하지 않고, 벡터와 단어 검색 후보를 가장 넓은 범위로 한 번 모아 앞에서부터 잘라 본다.
     */
    public SectionedSearchHits search(String query, ResourceTrack track) {
        validateQuery(query);
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

    private void validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("검색어는 비어 있을 수 없습니다.");
        }
    }
}

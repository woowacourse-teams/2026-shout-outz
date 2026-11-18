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

    public SectionedSearchHits search(String query, ResourceTrack track) {
        validateQuery(query);
        float[] queryVector = embeddingClient.embed(query);
        String modelId = embeddingClient.modelId();

        int candidateLimit = INITIAL_CANDIDATES;
        while (true) {
            HybridRanking ranking = hybridResourceRanker.rank(query, queryVector, modelId, track, candidateLimit);
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

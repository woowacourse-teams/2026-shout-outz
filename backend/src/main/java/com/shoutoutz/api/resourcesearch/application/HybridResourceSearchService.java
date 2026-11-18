package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.common.embedding.EmbeddingClient;
import com.shoutoutz.api.resourcesearch.application.dto.HybridRanking;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HybridResourceSearchService implements ResourceSearchService {

    private static final int INITIAL_CANDIDATES = 20;
    private static final int MAX_CANDIDATES = 400;

    private final EmbeddingClient embeddingClient;
    private final HybridResourceRanker hybridResourceRanker;

    @Override
    public List<SearchHit> search(String query, ResourceTrack track, int limit) {
        validateInput(query, limit);
        long startedAt = System.nanoTime();
        float[] queryVector = embeddingClient.embed(query);
        String modelId = embeddingClient.modelId();
        long embeddingNanos = System.nanoTime() - startedAt;
        int candidateLimit = Math.min(MAX_CANDIDATES, Math.max(INITIAL_CANDIDATES, limit));
        int attempts = 0;

        while (true) {
            HybridRanking ranking = hybridResourceRanker.rank(query, queryVector, modelId, track, candidateLimit);
            List<SearchHit> hits = ranking.hits().stream().limit(limit).toList();
            attempts++;

            if (hits.size() >= limit || ranking.exhausted() || candidateLimit == MAX_CANDIDATES) {
                log.debug("Resource search: embeddingMs={}, rankingMs={}, attempts={}, candidateLimit={}, documents={}",
                        embeddingNanos / 1_000_000.0,
                        (System.nanoTime() - startedAt - embeddingNanos) / 1_000_000.0,
                        attempts, candidateLimit, hits.size());
                return hits;
            }
            candidateLimit = Math.min(MAX_CANDIDATES, candidateLimit * 2);
        }
    }

    private void validateInput(String query, int limit) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("검색어는 비어 있을 수 없습니다.");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("검색 문서 수는 1 이상이어야 합니다.");
        }
    }
}

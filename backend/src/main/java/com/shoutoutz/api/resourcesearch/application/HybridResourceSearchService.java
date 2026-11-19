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

    /**
     * 한 문서의 조각이 여러 개 걸리면 조각 수보다 문서 수가 적다. 그래서 문서 limit 건이 찰 때까지 보는 범위를 두 배씩 넓힌다.
     * 범위를 넓힐 때마다 조회를 다시 하지 않고, 벡터와 단어 검색 후보를 가장 넓은 범위로 한 번 모아 앞에서부터 잘라 본다.
     */
    @Override
    public List<SearchHit> search(String query, ResourceTrack track, int limit) {
        validateInput(query, limit);
        long startedAt = System.nanoTime();
        float[] queryVector = embeddingClient.embed(query);
        String modelId = embeddingClient.modelId();
        long embeddingNanos = System.nanoTime() - startedAt;
        HybridCandidates candidates = hybridResourceRanker.collect(query, queryVector, modelId, track, MAX_CANDIDATES);
        long collectNanos = System.nanoTime() - startedAt - embeddingNanos;
        int candidateLimit = Math.min(MAX_CANDIDATES, Math.max(INITIAL_CANDIDATES, limit));

        while (true) {
            HybridRanking ranking = candidates.rank(candidateLimit);
            List<SearchHit> hits = ranking.hits().stream().limit(limit).toList();

            if (hits.size() >= limit || ranking.exhausted() || candidateLimit == MAX_CANDIDATES) {
                log.debug("Resource search: embeddingMs={}, collectMs={}, totalMs={}, candidateLimit={}, documents={}",
                        embeddingNanos / 1_000_000.0,
                        collectNanos / 1_000_000.0,
                        (System.nanoTime() - startedAt) / 1_000_000.0,
                        candidateLimit, hits.size());
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

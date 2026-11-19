package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.common.embedding.EmbeddingClient;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorResourceSearchService implements ResourceSearchService {

    private static final int INITIAL_CANDIDATES = 20;
    private static final int MAX_CANDIDATES = 400;

    private final EmbeddingClient embeddingClient;
    private final ResourceSearchRepository resourceSearchRepository;

    /**
     * 한 문서의 조각이 여러 개 걸리면 조각 수보다 문서 수가 적다. 그래서 문서 limit 건이 찰 때까지 보는 범위를 두 배씩 넓힌다.
     * 조회는 조각 전체를 읽으므로 범위를 넓힐 때마다 다시 하지 않고, 가장 넓은 범위를 한 번 읽어 앞에서부터 잘라 본다.
     * 가까운 순서가 항상 같아 앞 N개는 N개만 조회한 결과와 같다.
     */
    @Override
    public List<SearchHit> search(String query, ResourceTrack track, int limit) {
        validateInput(query, limit);
        long startedAt = System.nanoTime();
        float[] queryVector = embeddingClient.embed(query);
        String modelId = embeddingClient.modelId();
        long embeddingNanos = System.nanoTime() - startedAt;
        List<ResourceSearchCandidate> nearest = resourceSearchRepository.findNearest(
                queryVector, modelId, track, MAX_CANDIDATES
        );
        long databaseNanos = System.nanoTime() - startedAt - embeddingNanos;
        int candidateLimit = Math.min(MAX_CANDIDATES, Math.max(INITIAL_CANDIDATES, limit));

        while (true) {
            List<ResourceSearchCandidate> candidates = nearest.subList(0, Math.min(candidateLimit, nearest.size()));
            List<SearchHit> hits = SearchHits.rank(candidates).stream().limit(limit).toList();

            if (hits.size() >= limit || candidates.size() < candidateLimit || candidateLimit == MAX_CANDIDATES) {
                log.debug("Resource search: embeddingMs={}, databaseMs={}, totalMs={}, "
                                + "candidateLimit={}, candidates={}, documents={}",
                        embeddingNanos / 1_000_000.0,
                        databaseNanos / 1_000_000.0,
                        (System.nanoTime() - startedAt) / 1_000_000.0,
                        candidateLimit, candidates.size(), hits.size());
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

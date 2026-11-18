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

    @Override
    public List<SearchHit> search(String query, ResourceTrack track, int limit) {
        validateInput(query, limit);
        long startedAt = System.nanoTime();
        float[] queryVector = embeddingClient.embed(query);
        String modelId = embeddingClient.modelId();
        long embeddingNanos = System.nanoTime() - startedAt;
        long databaseNanos = 0;
        long aggregationNanos = 0;
        int candidateLimit = initialCandidateLimit(limit);
        int attempts = 0;

        while (true) {
            long databaseStartedAt = System.nanoTime();
            List<ResourceSearchCandidate> candidates = resourceSearchRepository.findNearest(
                    queryVector, modelId, track, candidateLimit
            );
            databaseNanos += System.nanoTime() - databaseStartedAt;
            long aggregationStartedAt = System.nanoTime();
            List<SearchHit> hits = SearchHits.rank(candidates).stream().limit(limit).toList();
            aggregationNanos += System.nanoTime() - aggregationStartedAt;
            attempts++;

            if (hits.size() >= limit || candidates.size() < candidateLimit || candidateLimit == MAX_CANDIDATES) {
                log.debug("Resource search: embeddingMs={}, databaseMs={}, aggregationMs={}, totalMs={}, "
                                + "attempts={}, candidateLimit={}, candidates={}, documents={}",
                        embeddingNanos / 1_000_000.0,
                        databaseNanos / 1_000_000.0,
                        aggregationNanos / 1_000_000.0,
                        (System.nanoTime() - startedAt) / 1_000_000.0,
                        attempts, candidateLimit, candidates.size(), hits.size());
                return hits;
            }
            candidateLimit = nextCandidateLimit(candidateLimit);
        }
    }

    /**
     * 문서를 limit 건 찾을 때 벡터 조회를 최대 몇 번 하는지. 후보를 두 배씩 늘리는 규칙을 그대로 따라 센다.
     * 검색에 걸릴 수 있는 최대 시간을 미리 계산하는 쪽에서 쓴다.
     */
    public static int maxAttempts(int limit) {
        int candidateLimit = initialCandidateLimit(limit);
        int attempts = 1;
        while (candidateLimit < MAX_CANDIDATES) {
            candidateLimit = nextCandidateLimit(candidateLimit);
            attempts++;
        }
        return attempts;
    }

    private static int initialCandidateLimit(int limit) {
        return Math.min(MAX_CANDIDATES, Math.max(INITIAL_CANDIDATES, limit));
    }

    private static int nextCandidateLimit(int candidateLimit) {
        return Math.min(MAX_CANDIDATES, candidateLimit * 2);
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

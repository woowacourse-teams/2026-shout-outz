package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.common.embedding.EmbeddingClient;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        int candidateLimit = Math.min(MAX_CANDIDATES, Math.max(INITIAL_CANDIDATES, limit));
        int attempts = 0;

        while (true) {
            long databaseStartedAt = System.nanoTime();
            List<ResourceSearchCandidate> candidates = resourceSearchRepository.findNearest(
                    queryVector, modelId, track, candidateLimit
            );
            databaseNanos += System.nanoTime() - databaseStartedAt;
            long aggregationStartedAt = System.nanoTime();
            List<SearchHit> hits = aggregate(candidates, limit);
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

    private List<SearchHit> aggregate(List<ResourceSearchCandidate> candidates, int limit) {
        Map<Long, List<ResourceSearchCandidate>> documents = new HashMap<>();
        for (ResourceSearchCandidate candidate : candidates) {
            documents.computeIfAbsent(candidate.documentId(), ignored -> new ArrayList<>()).add(candidate);
        }
        return documents.values().stream()
                .map(this::toSearchHit)
                .sorted(Comparator.comparingDouble(SearchHit::similarityScore).reversed()
                        .thenComparing(SearchHit::documentId))
                .limit(limit)
                .toList();
    }

    private SearchHit toSearchHit(List<ResourceSearchCandidate> candidates) {
        ResourceSearchCandidate document = candidates.getFirst();
        List<MatchedChunk> chunks = candidates.stream()
                .map(ResourceSearchCandidate::chunk)
                .sorted(Comparator.comparingDouble(MatchedChunk::similarityScore).reversed()
                        .thenComparing(MatchedChunk::chunkId))
                .toList();
        return new SearchHit(
                document.documentId(),
                document.externalKey(),
                document.projectId(),
                document.repo(),
                document.title(),
                document.summary(),
                document.source(),
                document.track(),
                document.url(),
                chunks.getFirst().similarityScore(),
                chunks
        );
    }
}

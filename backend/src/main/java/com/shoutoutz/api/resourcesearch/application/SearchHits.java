package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SearchHits {

    private SearchHits() {
    }

    static List<SearchHit> rank(List<ResourceSearchCandidate> candidates) {
        Map<Long, List<ResourceSearchCandidate>> documents = new LinkedHashMap<>();
        for (ResourceSearchCandidate candidate : candidates) {
            documents.computeIfAbsent(candidate.documentId(), ignored -> new ArrayList<>()).add(candidate);
        }
        return documents.values().stream()
                .map(SearchHits::toSearchHit)
                .sorted(Comparator.comparingDouble(SearchHit::similarityScore).reversed()
                        .thenComparing(SearchHit::documentId))
                .toList();
    }

    private static SearchHit toSearchHit(List<ResourceSearchCandidate> candidates) {
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

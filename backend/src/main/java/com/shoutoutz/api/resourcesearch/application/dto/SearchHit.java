package com.shoutoutz.api.resourcesearch.application.dto;

import java.util.List;

public record SearchHit(
        Long documentId,
        String externalKey,
        Long projectId,
        String repo,
        String title,
        String summary,
        String source,
        String track,
        String url,
        double similarityScore,
        List<MatchedChunk> matchedChunks
) {
}

package com.shoutoutz.api.resourcesearch.application.dto;

import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;

public record SearchHit(
        Long documentId,
        String externalKey,
        Long projectId,
        String repo,
        String title,
        String summary,
        String source,
        ResourceTrack track,
        String url,
        double similarityScore,
        List<MatchedChunk> matchedChunks
) {
}

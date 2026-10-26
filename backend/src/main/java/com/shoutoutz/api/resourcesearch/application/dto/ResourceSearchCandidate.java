package com.shoutoutz.api.resourcesearch.application.dto;

import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;

public record ResourceSearchCandidate(
        Long documentId,
        String externalKey,
        Long projectId,
        String repo,
        String title,
        String summary,
        String source,
        ResourceTrack track,
        String url,
        MatchedChunk chunk
) {
}

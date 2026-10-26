package com.shoutoutz.api.resourcesearch.application.dto;

public record ResourceSearchCandidate(
        Long documentId,
        String externalKey,
        Long projectId,
        String repo,
        String title,
        String summary,
        String source,
        String track,
        String url,
        MatchedChunk chunk
) {
}

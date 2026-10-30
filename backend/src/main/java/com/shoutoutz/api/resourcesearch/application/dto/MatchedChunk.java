package com.shoutoutz.api.resourcesearch.application.dto;

public record MatchedChunk(
        Long chunkId,
        String chunkKind,
        String text,
        double similarityScore
) {
}

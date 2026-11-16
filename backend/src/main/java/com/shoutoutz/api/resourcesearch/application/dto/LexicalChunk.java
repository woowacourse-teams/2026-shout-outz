package com.shoutoutz.api.resourcesearch.application.dto;

import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;

public record LexicalChunk(
        Long chunkId,
        String text,
        ResourceTrack track
) {

    public LexicalChunk {
        if (chunkId == null) {
            throw new IllegalArgumentException("조각 ID는 필수입니다.");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("조각 본문은 비어 있을 수 없습니다.");
        }
    }
}

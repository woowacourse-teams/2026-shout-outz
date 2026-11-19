package com.shoutoutz.api.resourcesearch.application.dto;

import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;

/**
 * @param resource 조각이 속한 자료. 색인에 함께 넣어 두면 DB를 다시 읽지 않고 자료 순위를 만들 수 있다
 */
public record LexicalChunk(
        Long chunkId,
        String text,
        ResourceTrack track,
        RankedResource resource
) {

    public LexicalChunk {
        if (chunkId == null) {
            throw new IllegalArgumentException("조각 ID는 필수입니다.");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("조각 본문은 비어 있을 수 없습니다.");
        }
        if (resource == null || resource.documentId() == null) {
            throw new IllegalArgumentException("조각이 속한 자료는 필수입니다.");
        }
    }
}

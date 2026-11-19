package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.LexicalChunk;
import com.shoutoutz.api.resourcesearch.application.dto.SearchCandidates;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;

public interface ResourceSearchRepository {

    /**
     * 질문 벡터와 가까운 조각과, 단어 검색이 찾은 조각 중 그 안에 없는 조각을 한 번의 조회로 읽는다.
     */
    SearchCandidates findCandidates(
            float[] queryVector,
            String modelId,
            ResourceTrack track,
            int candidateLimit,
            List<Long> lexicalChunkIds
    );

    List<LexicalChunk> findLexicalChunks(String modelId);
}

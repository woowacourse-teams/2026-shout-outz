package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.LexicalChunk;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;

public interface ResourceSearchRepository {

    List<ResourceSearchCandidate> findNearest(float[] queryVector, String modelId, ResourceTrack track, int candidateLimit);

    List<ResourceSearchCandidate> findByChunkIds(List<Long> chunkIds, float[] queryVector, String modelId);

    List<LexicalChunk> findLexicalChunks(String modelId);
}

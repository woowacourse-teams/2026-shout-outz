package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import java.util.List;

public interface ResourceSearchRepository {

    List<ResourceSearchCandidate> findNearest(float[] queryVector, String modelId, String track, int candidateLimit);
}

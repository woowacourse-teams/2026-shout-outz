package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;

public interface ResourceSearchService {

    List<SearchHit> search(String query, ResourceTrack track, int limit);
}

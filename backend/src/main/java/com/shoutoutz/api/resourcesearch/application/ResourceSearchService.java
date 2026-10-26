package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import java.util.List;

public interface ResourceSearchService {

    List<SearchHit> search(String query, String track, int limit);
}

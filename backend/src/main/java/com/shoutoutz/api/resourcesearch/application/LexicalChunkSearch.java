package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.LexicalMatch;
import com.shoutoutz.api.resourcesearch.application.dto.RankedResource;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;

public interface LexicalChunkSearch {

    List<LexicalMatch> search(String query, ResourceTrack track, int limit);

    /**
     * 검색어와 맞는 조각 limit 개가 속한 자료를 순위대로 돌려준다. 한 자료의 조각이 여러 개 걸려도 자료는 한 번만 담는다.
     */
    List<RankedResource> searchResources(String query, ResourceTrack track, int limit);
}

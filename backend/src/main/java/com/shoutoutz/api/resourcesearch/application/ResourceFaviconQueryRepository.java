package com.shoutoutz.api.resourcesearch.application;

import java.util.Collection;
import java.util.Map;

public interface ResourceFaviconQueryRepository {

    /**
     * 링크 미리보기 수집 때 원문 페이지에서 뽑은 파비콘 주소를 자료 ID별로 준다. 아직 수집 전이거나 없으면 빠진다.
     */
    Map<Long, String> findFaviconUrls(Collection<Long> resourceIds);
}

package com.shoutoutz.api.resourcesearch.application;

import java.util.Collection;
import java.util.Map;

public interface ResourceFaviconQueryRepository {

    /**
     * 링크 미리보기 수집 때 원문 페이지에서 뽑은 파비콘 주소를 자료 ID별로 준다. 다시 받는 중이거나 실패한 동안에도
     * 마지막으로 받은 값을 준다. 한 번도 받지 못했으면 빠진다.
     */
    Map<Long, String> findFaviconUrls(Collection<Long> resourceIds);
}

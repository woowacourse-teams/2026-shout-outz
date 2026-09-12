package com.shoutoutz.api.techtag.domain;

import java.util.List;

public interface TechTagRepository {

    /**
     * 선택 가능한(is_active = true) 태그를 displayName 오름차순으로 조회
     */
    List<TechTag> findAllActive();

    /**
     * 선택 가능한 태그 중, displayName 또는 slug 에 keyword 가 포함된 것을 대소문자를 무시하고, displayName 오름차순으로 조회
     */
    List<TechTag> findAllActiveByKeyword(String keyword);

    /**
     * 주어진 id 중 선택 가능한(is_active = true) 태그만 조회한다. 순서는 보장하지 않는다.
     * 없는 id 나 비활성 태그는 결과에서 빠지므로, 호출부는 개수를 비교해 검증한다.
     */
    List<TechTag> findAllActiveByIds(List<Long> ids);
}

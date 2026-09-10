package com.shoutoutz.api.techtag.domain;

import java.util.List;

public interface TechTagRepository {

    /**
     * 선택 가능한(is_active = true) 태그를 displayName 오름차순으로 조회
     */
    List<TechTag> findAllActive();

    /**
     * 선택 가능한 태그 중, displayName 또는 slug 에 keyword 가 포함된 것을
     * 대소문자를 무시하고 displayName 오름차순으로 조회
     */
    List<TechTag> findAllActiveByKeyword(String keyword);
}

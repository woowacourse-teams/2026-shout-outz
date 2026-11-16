package com.shoutoutz.api.resourcesearch.application;

public interface TechTopicNames {

    /**
     * 검색어가 기술 주제의 이름이나 별칭과 정확히 같은지
     */
    boolean contains(String keyword);
}

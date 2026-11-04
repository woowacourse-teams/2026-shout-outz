package com.shoutoutz.api.jobposting.domain;

/**
 * 자료가 역량을 실제로 뒷받침하는지 판정한 등급
 * 값 이름은 requirement_recommendations.verdict 의 CHECK 제약과 같아야 한다.
 */
public enum RecommendationVerdict {
    /** 역량을 직접 다룬다 */
    MATCH,
    /** 일부만 다룬다 */
    PARTIAL,
    /** 관련이 없다. 추천에서 빼지만 검색 점수 기준선을 찾으려고 저장한다 */
    NOT_MATCH
}

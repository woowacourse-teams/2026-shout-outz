package com.shoutoutz.api.jobposting.domain;

/**
 * 운영자가 공고를 다시 분석할 범위.
 */
public enum ReanalysisScope {

    /**
     * 실패한 역량만 다시 분석한다. 역량이 최종 실패해 공고가 실패했을 때 쓴다.
     */
    FAILED,

    /**
     * 역량은 그대로 두고 모든 역량을 검색부터 다시 분석한다. 이전 추천 자료는 지운다. 우테코 자료가 새로 적재됐을 때 쓴다.
     */
    ALL
}

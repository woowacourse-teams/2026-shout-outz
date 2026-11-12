package com.shoutoutz.api.resourcereport.domain;

/**
 * 자료 제보 종류
 */
public enum ResourceReportKind {

    /**
     * 기술 이야기에 있는 PR, 팀 문서가 필요 없다고 알린 제보
     */
    REMOVE,

    /**
     * 기술 이야기나 전체 경험 근거에 빠진 PR, 팀 문서의 주소를 알려 준 제보
     */
    ADD
}

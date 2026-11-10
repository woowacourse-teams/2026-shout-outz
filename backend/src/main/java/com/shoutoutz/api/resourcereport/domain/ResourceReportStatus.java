package com.shoutoutz.api.resourcereport.domain;

/**
 * 자료 제보 처리 상태
 */
public enum ResourceReportStatus {

    /**
     * 운영자가 아직 확인하지 않음
     */
    OPEN,

    /**
     * 제보를 반영하기로 함
     */
    ACCEPTED,

    /**
     * 제보를 반영하지 않기로 함
     */
    REJECTED
}

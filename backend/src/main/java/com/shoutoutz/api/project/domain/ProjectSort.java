package com.shoutoutz.api.project.domain;

/**
 * 프로젝트 목록 정렬 기준
 */
public enum ProjectSort {

    /**
     * 등록 시각 내림차순
     */
    LATEST,

    /**
     * 좋아요 수 내림차순. 좋아요 수가 같으면 등록 시각 내림차순이다.
     */
    POPULAR
}

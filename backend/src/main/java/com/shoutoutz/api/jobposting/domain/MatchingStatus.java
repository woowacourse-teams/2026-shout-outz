package com.shoutoutz.api.jobposting.domain;

/**
 * 공고와 역량 항목에 맞는 자료를 찾는 매칭 작업의 진행 상태
 * 등록할 때는 PENDING 으로만 저장하고, 나머지 상태는 매칭 작업이 쓴다.
 * 값 이름은 job_postings.status, job_requirements.status 의 CHECK 제약과 같아야 한다.
 */
public enum MatchingStatus {
    PENDING,
    PROCESSING,
    READY,
    FAILED
}

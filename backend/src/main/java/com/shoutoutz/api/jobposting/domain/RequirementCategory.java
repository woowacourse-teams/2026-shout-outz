package com.shoutoutz.api.jobposting.domain;

/**
 * 역량 항목이 공고의 어느 부분에서 나왔는지
 * 값 이름은 job_requirements.category 의 CHECK 제약과 같아야 한다.
 */
public enum RequirementCategory {
    /** 자격 요건 */
    REQUIRED,
    /** 우대 사항 */
    PREFERRED,
    /** 주요 업무나 기술 스택 */
    DUTY_OR_STACK
}

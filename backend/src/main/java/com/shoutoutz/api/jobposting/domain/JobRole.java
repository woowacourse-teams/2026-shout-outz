package com.shoutoutz.api.jobposting.domain;

/**
 * 공고의 직무
 * 값 이름은 job_postings.job_role 의 CHECK 제약과 같아야 한다.
 */
public enum JobRole {
    BACKEND,
    FRONTEND,
    ANDROID,
    IOS,
    DEVOPS,
    DATA,
    AI,
    ETC
}

package com.shoutoutz.api.jobposting.domain;

import java.util.Optional;

public interface JobPostingRepository {

    /**
     * 공고와 역량 항목을 함께 저장한다. 같은 주소의 공고가 이미 있으면 중복 예외를 던진다.
     */
    JobPosting save(JobPosting jobPosting);

    Optional<JobPosting> findById(long jobPostingId);

    Optional<Long> findIdBySourceUrl(JobPostingUrl sourceUrl);
}

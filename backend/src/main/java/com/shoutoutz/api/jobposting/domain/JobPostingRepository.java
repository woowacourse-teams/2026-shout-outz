package com.shoutoutz.api.jobposting.domain;

import java.time.Instant;
import java.util.Optional;

public interface JobPostingRepository {

    /**
     * 공고와 역량 항목을 함께 저장한다. 같은 주소의 공고가 이미 있으면 중복 예외를 던진다.
     */
    JobPosting save(JobPosting jobPosting);

    Optional<JobPosting> findById(long jobPostingId);

    Optional<Long> findIdBySourceUrl(JobPostingUrl sourceUrl);

    /**
     * 공고를 고치기 전에 잠그고 읽는다. 트랜잭션이 끝날 때까지 분석 Worker 가 그 공고의 역량을 저장하지 못한다.
     * {@link #update} 와 함께 호출하는 쪽의 한 트랜잭션 안에서 불러야 한다. 따로 부르면 읽은 뒤 잠금이 풀려 그 사이 바뀐 값을 덮어쓴다.
     */
    Optional<JobPosting> findByIdForUpdate(long jobPostingId);

    /**
     * {@link #findByIdForUpdate} 로 읽어 고친 공고를 같은 트랜잭션 안에서 저장한다.
     * replaceRequirements 면 역량을 지우고 새로 넣으며, 지운 역량의 추천 자료도 함께 지워진다.
     */
    void update(JobPosting jobPosting, boolean replaceRequirements);

    /**
     * 숨김 시각만 바꾸고 분석 상태와 역량은 건드리지 않는다.
     * 숨길 때 이미 숨긴 공고면 처음 숨긴 시각을 그대로 두고, 해제하면 비운다. 공고가 없으면 빈 값을 돌려준다.
     */
    Optional<JobPostingVisibility> updateHiddenAt(long jobPostingId, boolean hidden, Instant now);

    /**
     * 공고를 지우면 역량과 추천 자료도 함께 지워진다. 공고가 없으면 false 를 돌려준다.
     */
    boolean deleteById(long jobPostingId);
}

package com.shoutoutz.api.jobposting.presentation.dto.response;

import com.shoutoutz.api.jobposting.domain.JobPosting;
import com.shoutoutz.api.jobposting.domain.MatchingStatus;

/**
 * reanalyzed 가 false 면 원문이 같아 공고 정보만 고친 것이다. 이때 요청의 역량 수정은 반영되지 않는다.
 */
public record AdminJobPostingUpdateResponse(
        long jobPostingId,
        MatchingStatus status,
        boolean reanalyzed
) {

    public static AdminJobPostingUpdateResponse of(JobPosting jobPosting, boolean reanalyzed) {
        return new AdminJobPostingUpdateResponse(
                jobPosting.getId(),
                jobPosting.getStatus(),
                reanalyzed
        );
    }
}

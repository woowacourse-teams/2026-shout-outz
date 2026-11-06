package com.shoutoutz.api.jobposting.presentation.dto.response;

import com.shoutoutz.api.jobposting.domain.MatchingStatus;

/**
 * 다시 분석을 받은 공고는 항상 PENDING 이다. reanalyzedRequirementCount 는 대기로 되돌린 역량 수다.
 */
public record AdminJobPostingReanalysisResponse(
        long jobPostingId,
        MatchingStatus status,
        int reanalyzedRequirementCount
) {

    public static AdminJobPostingReanalysisResponse pending(long jobPostingId, int reanalyzedRequirementCount) {
        return new AdminJobPostingReanalysisResponse(jobPostingId, MatchingStatus.PENDING, reanalyzedRequirementCount);
    }
}

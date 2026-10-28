package com.shoutoutz.api.jobposting.application.dto;

import com.shoutoutz.api.jobposting.domain.RecommendationVerdict;

/**
 * 후보 자료 하나를 판정한 결과. description 은 NOT_MATCH 이면 비어 있다 (어차피 추천에서 빠진다).
 */
public record RequirementJudgement(
        String source,
        String externalKey,
        RecommendationVerdict verdict,
        String reason,
        String description
) {
}

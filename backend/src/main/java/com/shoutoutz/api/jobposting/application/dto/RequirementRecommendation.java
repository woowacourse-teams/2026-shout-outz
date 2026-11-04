package com.shoutoutz.api.jobposting.application.dto;

import com.shoutoutz.api.jobposting.domain.RecommendationVerdict;
import java.util.List;

/**
 * 역량에 저장할 판정 결과 하나. 후보 자료와 그 자료의 판정 결과를 합친 것이다.
 * evidence 는 판정할 때 LLM 에게 보여 준 조각이다. 자료가 다시 적재되면 조각이 바뀌어서 복사해 둔다.
 */
public record RequirementRecommendation(
        String source,
        String externalKey,
        RecommendationVerdict verdict,
        String reason,
        String description,
        double similarityScore,
        List<CandidateChunk> evidence
) {
    public RequirementRecommendation {
        evidence = List.copyOf(evidence);
    }
}

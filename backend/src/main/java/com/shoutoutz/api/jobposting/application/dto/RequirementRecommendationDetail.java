package com.shoutoutz.api.jobposting.application.dto;

import com.shoutoutz.api.jobposting.domain.RecommendationVerdict;
import java.util.List;

/**
 * 관리자 화면에 보여 줄 판정 결과 하나. 저장된 판정에 자료의 제목과 링크를 합친 것이다.
 * 자료가 없거나 삭제됐으면 title, url 은 null 이고 판정 내용은 그대로 있다.
 */
public record RequirementRecommendationDetail(
        long requirementId,
        String source,
        String externalKey,
        String title,
        String url,
        RecommendationVerdict verdict,
        String reason,
        String description,
        double similarityScore,
        List<CandidateChunk> evidence
) {
    public RequirementRecommendationDetail {
        evidence = List.copyOf(evidence);
    }
}

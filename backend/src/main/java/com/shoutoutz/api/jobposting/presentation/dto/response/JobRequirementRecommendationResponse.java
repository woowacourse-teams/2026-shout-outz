package com.shoutoutz.api.jobposting.presentation.dto.response;

import com.shoutoutz.api.jobposting.application.dto.RecommendedResource;
import com.shoutoutz.api.jobposting.domain.RecommendationVerdict;
import java.time.Instant;

/**
 * 역량별 추천 자료 카드 한 장. source, contentType 은 코드 그대로 주고 화면 문구는 프론트가 정한다.
 */
public record JobRequirementRecommendationResponse(
        RecommendationVerdict verdict,
        String source,
        String title,
        String url,
        String description,
        Instant publishedAt,
        String contentType,
        String teamName,
        String authorDisplay
) {

    public static JobRequirementRecommendationResponse from(RecommendedResource resource) {
        return new JobRequirementRecommendationResponse(
                resource.verdict(),
                resource.source(),
                resource.title(),
                resource.url(),
                resource.description(),
                resource.publishedAt(),
                resource.contentType(),
                resource.teamName(),
                resource.authorDisplay()
        );
    }
}

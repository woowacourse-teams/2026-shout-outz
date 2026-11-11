package com.shoutoutz.api.jobposting.application.dto;

import com.shoutoutz.api.jobposting.domain.RecommendationVerdict;
import java.time.Instant;

/**
 * 사용자에게 보여 줄 역량별 추천 자료 한 건. 자료 종류와 문서 종류는 데이터 파이프라인이 넣은 값 그대로다.
 *
 * @param resourceId 자료(resource_documents) ID
 * @param teamName 자료가 연결된 프로젝트의 팀 이름. 연결된 프로젝트가 없으면 null
 */
public record RecommendedResource(
        long resourceId,
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
}

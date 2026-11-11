import type { Material } from '@/apis/explore';
import type { JobRequirementRecommendation } from '@/apis/job-posting';

/**
 * 역량별 추천 자료를 기술 자료 카드 모양으로 옮긴다. 탐색 화면의 목록과 자료 요약 모달을 그대로 쓰기 위해서다.
 * 추천 자료 응답에는 한줄 소개 대신 역량과의 관계 설명(description)이 있어 그것을 한줄 소개 자리에 보여 준다.
 */
export function recommendationToMaterial(recommendation: JobRequirementRecommendation): Material {
  return {
    resourceId: recommendation.resourceId,
    source: recommendation.source,
    title: recommendation.title,
    url: recommendation.url,
    summary: recommendation.description,
    publishedAt: recommendation.publishedAt,
    platform: null,
    team: recommendation.teamName
      ? { name: recommendation.teamName, projectSlug: null, logoMediaId: null, logoUrl: null }
      : undefined,
  };
}

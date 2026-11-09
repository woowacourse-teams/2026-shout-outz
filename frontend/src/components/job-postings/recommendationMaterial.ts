import type { Material, MaterialKind } from '@/apis/explore';
import type { JobRequirementRecommendation } from '@/apis/job-posting';
import { blogFaviconOf } from '@/components/explore/MaterialParts';
import { youtubeId } from '@/pages/about/resources';

const KIND_BY_SOURCE: Record<string, MaterialKind> = {
  'blog-post': 'BLOG',
  'github-pr': 'PR',
  'github-wiki': 'TEAM_DOC',
  'youtube-tekotalk': 'TECH_TALK',
};

/** 역량별 추천 자료를 탐색 화면의 자료 모양으로 바꾼다. 같은 목록과 자료 요약 모달을 그대로 쓰기 위해서다. */
export function recommendationToMaterial(recommendation: JobRequirementRecommendation): Material {
  // 화면이 모르는 source 코드는 블로그로 보여 준다.
  const kind = KIND_BY_SOURCE[recommendation.source] ?? 'BLOG';
  const videoId = kind === 'TECH_TALK' ? youtubeId(recommendation.url) : undefined;

  return {
    id: String(recommendation.resourceId),
    kind,
    title: recommendation.title,
    url: recommendation.url,
    publishedAt: recommendation.publishedAt?.slice(0, 10) ?? null,
    summary: recommendation.description,
    project:
      kind !== 'TECH_TALK' && recommendation.teamName
        ? { name: recommendation.teamName, logoUrl: null }
        : null,
    faviconUrl: kind === 'BLOG' ? blogFaviconOf(recommendation.url) : null,
    speaker: kind === 'TECH_TALK' ? recommendation.authorDisplay : null,
    thumbnailUrl: videoId ? `https://i.ytimg.com/vi/${videoId}/mqdefault.jpg` : null,
  };
}

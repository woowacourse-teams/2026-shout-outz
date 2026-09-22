import type { HomeBanner } from '@/types/home';

/**
 * 배너가 눌렸을 때 갈 곳.
 *
 * `internal`은 라우터로 이동하고, `external`은 새 탭으로 연다.
 * 갈 곳을 정할 수 없으면 `null`이고, 이때 배너는 링크가 아니라 이미지로만 남는다.
 */
export type HomeBannerLink =
  { kind: 'internal'; href: string } | { kind: 'external'; href: string };

/** 대상 리소스 유형별 상세 경로. 라우트(`/news/$newsId` 등)와 같은 접두사를 쓴다. */
const TARGET_SEGMENTS = {
  NEWS: 'news',
  PROJECT: 'projects',
  FEED: 'feeds',
} as const;

const isInternalPath = (url: string) => url.startsWith('/') && !url.startsWith('//');

const isHttpUrl = (url: string) => /^https?:\/\//i.test(url);

/**
 * 배너 응답을 이동할 링크로 바꾼다.
 *
 * 서버는 이동 방식을 두 갈래로 준다.
 * - `TARGET`: 서비스 안의 리소스를 가리킨다. `targetType`과 `targetId`로 상세 경로를 만든다.
 * - `URL`: 주소를 직접 준다. `linkType`이 내부 경로인지 외부 주소인지 알려준다.
 *
 * `linkType`이 둘 중 하나가 아니면 이동하지 않는다. 주소 모양으로 짐작하지 않는다.
 * `javascript:` 같은 주소가 그대로 렌더되지 않도록 http(s)와 내부 경로만 통과시킨다.
 */
export function resolveHomeBannerLink(banner: HomeBanner): HomeBannerLink | null {
  if (banner.destinationType === 'TARGET') {
    const segment = banner.targetType ? TARGET_SEGMENTS[banner.targetType] : undefined;
    if (!segment || banner.targetId == null) return null;

    return { kind: 'internal', href: `/${segment}/${banner.targetId}` };
  }

  const url = banner.linkUrl?.trim();
  if (!url) return null;

  if (banner.linkType === 'INTERNAL_PATH') {
    return isInternalPath(url) ? { kind: 'internal', href: url } : null;
  }
  if (banner.linkType === 'EXTERNAL_URL') {
    return isHttpUrl(url) ? { kind: 'external', href: url } : null;
  }

  return null;
}

import { useSuspenseQuery } from '@tanstack/react-query';

import { homeBannersQueryOptions } from '@/api/home';
import { HeroBanner } from '@/components/home/HeroBanner';

/**
 * 홈 상단 배너 영역.
 *
 * 응답은 표시 순서대로 정렬된 활성 배너 목록이지만 디자인의 배너 자리는 하나라 맨 앞 배너만 그린다.
 * 활성 배너가 없으면 빈 자리를 남기지 않고 아무것도 그리지 않는다.
 * 여러 배너를 돌려 보여주는 캐러셀은 디자인이 나오면 여기서 붙인다.
 */
export function HomeBannerSection() {
  const { data: banners } = useSuspenseQuery(homeBannersQueryOptions());
  const [banner] = banners;

  if (!banner) return null;

  return <HeroBanner banner={banner} />;
}

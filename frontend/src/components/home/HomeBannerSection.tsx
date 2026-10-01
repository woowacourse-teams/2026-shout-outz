import { useSuspenseQuery } from '@tanstack/react-query';

import { homeBannersQueryOptions } from '@/api/home';
import { BannerCarousel } from '@/components/home/BannerCarousel';

/**
 * 홈 상단 배너 영역.
 *
 * API의 표시 순서를 유지하며, 배너 수와 관계없이 캐러셀 영역을 보여준다.
 */
export function HomeBannerSection() {
  const { data: banners } = useSuspenseQuery(homeBannersQueryOptions());
  return <BannerCarousel key={banners.map((item) => item.bannerId).join(',')} banners={banners} />;
}

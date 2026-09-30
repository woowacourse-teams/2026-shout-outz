import { useSuspenseQuery } from '@tanstack/react-query';

import { homeBannersQueryOptions } from '@/api/home';
import { HeroBanner } from '@/components/home/HeroBanner';
import { BannerCarousel } from '@/components/home/BannerCarousel';

/**
 * 홈 상단 배너 영역.
 *
 * API의 표시 순서를 유지하며, 활성 배너가 여러 개면 캐러셀로 보여준다.
 */
export function HomeBannerSection() {
  const { data: banners } = useSuspenseQuery(homeBannersQueryOptions());
  const [banner] = banners;

  if (!banner) return null;

  if (banners.length === 1) return <HeroBanner banner={banner} />;

  return <BannerCarousel key={banners.map((item) => item.bannerId).join(',')} banners={banners} />;
}

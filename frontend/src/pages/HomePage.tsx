import { Footer } from '@/components/Footer';
import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/feeds/AsyncBoundary';
import { HeroBanner, type HeroBannerProps } from '@/components/home/HeroBanner';
import { HomeEventSection } from '@/components/home/HomeEventSection';
import { HomeFeedSection } from '@/components/home/HomeFeedSection';
import { HomeStatistics } from '@/components/home/HomeStatistics';

// TODO 배너 API 명세가 나오면 조회 결과로 대체. 현재는 이미지가 없어 회색 배경과 대체 텍스트만 렌더
const HERO_BANNER: HeroBannerProps = {
  thumbnailUrl: '/images/home-banner-thumbnail.webp',
  originalUrl: '/images/home-banner-original.webp',
  alt: '금주의 추천 프로젝트',
  href: '/projects',
};

export function HomePage() {
  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <title>shout-outz</title>
      <AppGnb />
      <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-6 px-4 pt-6 pb-12 md:gap-8 md:pt-10 md:pb-20">
        <HeroBanner {...HERO_BANNER} />
        <AsyncBoundary>
          <HomeStatistics />
        </AsyncBoundary>

        <div className="flex flex-col gap-8 lg:flex-row lg:items-start lg:gap-12">
          <div className="min-w-0 lg:flex-1">
            <AsyncBoundary>
              <HomeFeedSection />
            </AsyncBoundary>
          </div>
          <div className="lg:w-93 lg:shrink-0">
            <AsyncBoundary>
              <HomeEventSection />
            </AsyncBoundary>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}

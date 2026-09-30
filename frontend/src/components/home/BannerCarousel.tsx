import { useEffect, useState } from 'react';
import {
  IconChevronLeft,
  IconChevronRight,
  IconPlayerPause,
  IconPlayerPlay,
} from '@tabler/icons-react';

import { Button } from '@/components/Button';
import { HeroBanner } from '@/components/home/HeroBanner';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import type { HomeBanner } from '@/types/home';
import { cn } from '@/utils/cn';

const ROTATION_INTERVAL = 5_000;

export function BannerCarousel({ banners }: { banners: HomeBanner[] }) {
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const [hovered, setHovered] = useState(false);
  const [focused, setFocused] = useState(false);
  const [pageHidden, setPageHidden] = useState(() =>
    typeof document !== 'undefined' ? document.hidden : false,
  );
  const reducedMotion = useMediaQuery('(prefers-reduced-motion: reduce)');
  const rotating = !paused && !hovered && !focused && !pageHidden && !reducedMotion;

  useEffect(() => {
    const updateVisibility = () => setPageHidden(document.hidden);
    document.addEventListener('visibilitychange', updateVisibility);
    return () => document.removeEventListener('visibilitychange', updateVisibility);
  }, []);

  useEffect(() => {
    if (!rotating) return;
    const timer = window.setTimeout(
      () => setIndex((current) => (current + 1) % banners.length),
      ROTATION_INTERVAL,
    );
    return () => window.clearTimeout(timer);
  }, [rotating, index, banners.length]);

  const move = (step: number) =>
    setIndex((current) => (current + step + banners.length) % banners.length);

  return (
    <section
      aria-label="홈 배너 모음"
      aria-roledescription="캐러셀"
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onFocusCapture={() => setFocused(true)}
      onBlurCapture={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) setFocused(false);
      }}
      className="min-w-0"
    >
      <div className="overflow-hidden rounded-2xl">
        <div
          className="flex transition-transform duration-500 ease-out motion-reduce:transition-none"
          style={{ transform: `translateX(-${index * 100}%)` }}
        >
          {banners.map((banner, position) => (
            <div
              key={banner.bannerId}
              role="group"
              aria-roledescription="슬라이드"
              aria-label={`${position + 1} / ${banners.length}`}
              aria-hidden={position !== index}
              inert={position !== index}
              className="w-full min-w-0 shrink-0"
            >
              <HeroBanner banner={banner} className="focus-visible:-outline-offset-2" />
            </div>
          ))}
        </div>
      </div>

      <div className="mt-3 flex items-center gap-3">
        <div role="group" aria-label="배너 위치 선택" className="flex min-w-0 flex-1 gap-1">
          {banners.map((banner, position) => (
            <button
              key={banner.bannerId}
              type="button"
              aria-label={`${position + 1}번 배너 보기`}
              aria-current={position === index ? 'true' : undefined}
              onClick={() => setIndex(position)}
              className="focus-visible:outline-primary-600 flex h-8 min-w-0 flex-1 cursor-pointer items-center rounded-sm focus-visible:outline-2"
            >
              <span
                className={cn(
                  'h-1 w-full rounded-full',
                  position === index ? 'bg-primary-500' : 'bg-gray-200',
                )}
              />
            </button>
          ))}
        </div>
        <span
          aria-live={rotating ? 'off' : 'polite'}
          aria-atomic="true"
          className="shrink-0 text-xs text-gray-600 tabular-nums"
        >
          <span className="sr-only">현재 배너 </span>
          {index + 1} / {banners.length}
        </span>
        <div className="flex shrink-0 items-center gap-1">
          {!reducedMotion && (
            <Button
              variant="ghost"
              size="sm"
              className="px-2"
              aria-label={paused ? '배너 자동 재생' : '배너 자동 재생 일시정지'}
              onClick={() => setPaused((current) => !current)}
            >
              {paused ? (
                <IconPlayerPlay size={16} aria-hidden="true" />
              ) : (
                <IconPlayerPause size={16} aria-hidden="true" />
              )}
            </Button>
          )}
          <Button
            variant="ghost"
            size="sm"
            className="px-2"
            aria-label="이전 배너"
            onClick={() => move(-1)}
          >
            <IconChevronLeft size={16} aria-hidden="true" />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="px-2"
            aria-label="다음 배너"
            onClick={() => move(1)}
          >
            <IconChevronRight size={16} aria-hidden="true" />
          </Button>
        </div>
      </div>
    </section>
  );
}

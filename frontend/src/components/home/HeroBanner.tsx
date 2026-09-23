import { Link } from '@tanstack/react-router';

import { type HomeBanner } from '@/types/home';
import { cn } from '@/utils/cn';
import { resolveHomeBannerLink } from '@/utils/home-banner';
import { analytics } from '@/utils/analytics';

/**
 * 홈 상단 배너.
 *
 * 응답에는 이미지 URL과 이동 정보만 있고 대체 텍스트가 없다. 배너 문구는 이미지 안에 그려져 있어서
 * 읽을 방법이 없으므로 공통 문구를 쓴다. 서버가 대체 텍스트를 주면 그 값으로 바꾼다.
 *
 * 갈 곳을 정할 수 없는 배너는 링크 없이 이미지만 보여준다.
 */
export interface HeroBannerProps {
  banner: HomeBanner;
  className?: string;
}

const BANNER_LABEL = '홈 배너';

const FRAME =
  'focus-visible:outline-primary-600 block h-59 overflow-hidden rounded-2xl bg-gray-100 focus-visible:outline-2 focus-visible:outline-offset-2 md:h-57.5';

export function HeroBanner({ banner, className }: HeroBannerProps) {
  const link = resolveHomeBannerLink(banner);
  const image = <img src={banner.imageUrl} alt={BANNER_LABEL} className="size-full object-cover" />;

  if (!link) {
    return <div className={cn(FRAME, className)}>{image}</div>;
  }

  if (link.kind === 'external') {
    return (
      <a
        href={link.href}
        target="_blank"
        rel="noopener noreferrer"
        className={cn(FRAME, className)}
        onClick={() => analytics.track({ name: 'hero_banner_clicked' })}
      >
        {image}
      </a>
    );
  }

  return (
    <Link
      to={link.href}
      className={cn(FRAME, className)}
      onClick={() => analytics.track({ name: 'hero_banner_clicked' })}
    >
      {image}
    </Link>
  );
}

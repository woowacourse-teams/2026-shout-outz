import type { ComponentProps } from 'react';
import { cn } from '@/utils/cn';
import { analytics } from '@/utils/analytics';

// 작은 화면에서는 썸네일을, `md` 이상에서는 원본 이미지 사용
// TODO 배너 API 명세 확정 후 필드명 대체
export interface HeroBannerProps extends Omit<ComponentProps<'a'>, 'children' | 'href'> {
  thumbnailUrl: string;
  originalUrl: string;
  alt: string;
  href: string;
}

export function HeroBanner({
  thumbnailUrl,
  originalUrl,
  alt,
  href,
  className,
  ...props
}: HeroBannerProps) {
  return (
    <a
      href={href}
      onClick={() => analytics.track({ name: 'hero_banner_clicked' })}
      className={cn(
        'focus-visible:outline-primary-600 block h-59 overflow-hidden rounded-2xl bg-gray-100 focus-visible:outline-2 focus-visible:outline-offset-2 md:h-57.5',
        className,
      )}
      {...props}
    >
      <picture className="block size-full">
        <source media="(min-width: 768px)" srcSet={originalUrl} />
        <img src={thumbnailUrl} alt={alt} className="size-full object-cover" />
      </picture>
    </a>
  );
}

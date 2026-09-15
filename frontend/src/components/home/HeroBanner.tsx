import type { ComponentProps } from 'react';
import { cn } from '@/utils/cn';

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
      className={cn(
        'focus-visible:outline-primary-600 block overflow-hidden rounded-2xl bg-gray-100 focus-visible:outline-2 focus-visible:outline-offset-2',
        className,
      )}
      {...props}
    >
      <picture>
        <source media="(min-width: 768px)" srcSet={originalUrl} />
        <img src={thumbnailUrl} alt={alt} className="size-full object-cover" />
      </picture>
    </a>
  );
}

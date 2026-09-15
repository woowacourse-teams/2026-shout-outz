import type { ComponentProps } from 'react';

// TODO 배너 API 명세 확정 후 props 결정 (이미지 URL 직접 수신인지 mediaId인지, 링크 유무)
export interface HeroBannerProps extends Omit<ComponentProps<'a'>, 'children' | 'href'> {
  imageUrl: string;
  alt: string;
  href: string;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars -- TODO 테스트 검토 후 구현
export function HeroBanner(props: HeroBannerProps) {
  return null;
}

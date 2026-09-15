import type { ComponentProps } from 'react';

/**
 * 홈 상단 이미지 배너. 이미지 API가 썸네일용과 원본 이미지 URL을 나눠 준다.
 */
// TODO 배너 API 명세 확정 후 필드명 대체
export interface HeroBannerProps extends Omit<ComponentProps<'a'>, 'children' | 'href'> {
  thumbnailUrl: string;
  originalUrl: string;
  alt: string;
  href: string;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars -- TODO 테스트 검토 후 구현
export function HeroBanner(props: HeroBannerProps) {
  return null;
}

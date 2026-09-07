import type { ComponentProps } from 'react';

import { cn } from '@/utils/cn';

/**
 * 지름 단계.
 *
 * - `xs`: 20px — 목록 미리보기, 댓글
 * - `sm`: 28px — 카드 작성자
 * - `md`: 32px — 헤더, 데스크톱 작성자
 * - `lg`: 52px — 프로필 헤더
 */
export type AvatarSize = 'xs' | 'sm' | 'md' | 'lg';

/**
 * 원형 이미지 박스만 담당한다. 표시할 이미지는 호출부가 `src`로 넘긴다.
 *
 * `src`가 없으면 `primary-50` 배경의 빈 원이 남는다. 배경은 항상 깔려 있고
 * 이미지가 그 위를 덮는 구조라 별도 분기가 없다.
 *
 * `width`와 `height`는 `size`와 충돌하므로 가린다.
 */
export interface AvatarProps extends Omit<ComponentProps<'img'>, 'width' | 'height' | 'alt'> {
  /** @default 'md' */
  size?: AvatarSize;
  /**
   * 옆에 이름이 함께 노출되면 빈 문자열을 넘겨 중복 낭독을 막는다.
   * 아바타만 단독으로 쓰이면 사람 이름을 넣는다.
   */
  alt: string;
}

const BASE = 'shrink-0 rounded-full bg-primary-50 object-cover';

const SIZE_CLASSES: Record<AvatarSize, string> = {
  xs: 'size-5',
  sm: 'size-7',
  md: 'size-8',
  lg: 'size-13',
};

export function Avatar({ size = 'md', className, ...props }: AvatarProps) {
  return <img className={cn(BASE, SIZE_CLASSES[size], className)} {...props} />;
}

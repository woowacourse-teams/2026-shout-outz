import type { ComponentProps } from 'react';

import { cn } from '@/utils/cn';

/**
 * 채움 강도.
 *
 * - `soft`: 옅은 배경에 같은 계열의 진한 글자
 * - `solid`: 진한 배경에 흰 글자
 *
 */
export type BadgeVariant = 'soft' | 'solid';

/**
 * 색 갈래.
 *
 * - `gray`: 기술 스택처럼 읽고 지나가는 메타
 * - `primary`: 소속·역할(6기 백엔드, 작성자), 강조 분류(공지사항)
 * - `green`: 이벤트
 */
export type BadgeTone = 'gray' | 'primary' | 'green';

/**
 * ```tsx
 * <Badge>React</Badge>
 * <Badge tone="primary">6기 백엔드</Badge>
 * <Badge variant="solid" tone="green">이벤트</Badge>
 * <Badge className="rounded-full bg-white/20 text-white">금주의 추천 프로젝트</Badge>
 * ```
 */
export interface BadgeProps extends ComponentProps<'span'> {
  /** @default 'soft' */
  variant?: BadgeVariant;
  /** @default 'gray' */
  tone?: BadgeTone;
}

const BASE = 'inline-flex items-center rounded-sm px-1.5 py-0.5 text-xs whitespace-nowrap';

const FILL_CLASSES: Record<BadgeVariant, Record<BadgeTone, string>> = {
  soft: {
    gray: 'bg-gray-100 text-gray-600',
    primary: 'bg-primary-50 text-primary-600',
    green: 'bg-green-50 text-green-600',
  },
  solid: {
    gray: 'bg-gray-600 text-white',
    primary: 'bg-primary-600 text-white',
    green: 'bg-green-600 text-white',
  },
};

const WEIGHT_CLASSES: Record<BadgeTone, string> = {
  gray: 'font-normal',
  primary: 'font-bold',
  green: 'font-bold',
};

export function Badge({ variant = 'soft', tone = 'gray', className, ...props }: BadgeProps) {
  return (
    <span
      className={cn(BASE, FILL_CLASSES[variant][tone], WEIGHT_CLASSES[tone], className)}
      {...props}
    />
  );
}

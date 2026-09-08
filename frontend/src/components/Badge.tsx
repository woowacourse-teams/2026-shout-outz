import type { ComponentProps } from 'react';

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

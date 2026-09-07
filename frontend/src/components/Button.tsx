import type { ComponentProps } from 'react';

import { cn } from '@/utils/cn';

/**
 * 시각적 형태.
 *
 * - `primary`: 배경 `primary-600`, 흰 텍스트
 * - `secondary`: 배경 `gray-100`, 테두리 `gray-200`
 * - `outline`: 배경 없음, 테두리 `gray-200`
 * - `ghost`: 배경·테두리 없음
 *
 * 역할에 따라 정해진다.
 */
export type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost';

/**
 * 위계 단계.
 *
 * - `sm`: 32px — 카드 안 인라인 액션
 * - `md`: 40px — 폼·모달 액션
 * - `lg`: 48px — 화면 하단 전체폭 액션 바
 *
 * 크기가 아니라 주변 콘텐츠 대비 위계를 고른다.
 */
export type ButtonSize = 'sm' | 'md' | 'lg';

export interface ButtonVariants {
  /** @default 'primary' */
  variant?: ButtonVariant;
  /** @default 'md' */
  size?: ButtonSize;
}

/**
 * 너비는 prop으로 두지 않으며 `w-full`도 넣지 않는다. 기본은 fit-content이고,
 * 전체폭이 필요한 곳에서 `className`으로 `w-full` 또는 `flex-1`을 지정한다.
 *
 * disabled 상태는 별도 prop 없이 `disabled` 네이티브 속성으로 표현한다.
 *
 * `type`은 `'button'`을 기본값으로 둔다. 네이티브 기본값인 `'submit'`은 form
 * 안의 취소 버튼이 의도치 않게 폼을 제출하게 만든다.
 */
export interface ButtonProps extends ComponentProps<'button'>, ButtonVariants {}

/**
 * `<button>`이 아닌 요소에 버튼 스타일을 입힐 때 사용한다.
 *
 * 의미상 링크인 버튼(GitHub 로그인, 서비스 바로가기, 메인 홈으로 이동 등)은
 * `<a>`나 라우터의 `Link`로 렌더해야 하므로 스타일만 따로 가져간다.
 *
 * ```tsx
 * <a href="https://github.com/login/oauth" className={getButtonStyles({ size: 'lg' })}>
 *   GitHub 계정으로 시작하기
 * </a>
 * ```
 */
export type ButtonStylesOptions = ButtonVariants & { className?: string };

const BASE =
  'inline-flex cursor-pointer items-center justify-center rounded-lg whitespace-nowrap transition-colors focus-visible:ring-2 focus-visible:ring-primary-600 focus-visible:outline-none';

const VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary: 'bg-primary-600 font-bold text-white hover:bg-primary-700',
  secondary: 'border border-gray-200 bg-gray-100 font-medium text-gray-600 hover:bg-gray-200',
  outline: 'border border-gray-200 font-bold text-gray-600 hover:bg-gray-50',
  ghost: 'font-medium text-gray-600 hover:bg-gray-100',
};

const SIZE_CLASSES: Record<ButtonSize, string> = {
  sm: 'h-8 px-3 text-xs',
  md: 'h-10 px-5 text-sm',
  lg: 'h-12 px-6 text-base',
};

export function getButtonStyles({
  variant = 'primary',
  size = 'md',
  className,
}: ButtonStylesOptions = {}) {
  return cn(BASE, VARIANT_CLASSES[variant], SIZE_CLASSES[size], className);
}

export function Button({ variant, size, className, type = 'button', ...props }: ButtonProps) {
  return (
    <button type={type} className={getButtonStyles({ variant, size, className })} {...props} />
  );
}

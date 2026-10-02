import type { ComponentProps } from 'react';

import { cn } from '@/utils/cn';

/**
 * 시각적 형태.
 *
 * - `filled`: 배경 `gray-100`, 테두리 없음
 * - `outlined`: 배경 없음, 테두리 `gray-200`
 *
 * 역할에 따라 정해지며 뷰포트와 무관하다.
 */
export type InputVariant = 'filled' | 'outlined';

/**
 * 높이 단계.
 *
 * - `sm`: 40px
 * - `md`: 44px
 * - `lg`: 48px
 *
 * 반응형 값을 받지 않는다. 화면 폭에 따라 높이가 달라져야 하는 경우
 * 호출부에서 `className`으로 덮는다.
 */
export type InputSize = 'sm' | 'md' | 'lg';

/**
 * 네이티브 `<input>`의 `size`는 문자 수 기준 너비를 뜻하므로 가린다.
 *
 * 값 상태는 네이티브 `<input>`에 위임하므로 제어·비제어 모두 그대로 동작한다.
 * error, readonly 상태는 별도 prop 없이 `aria-invalid`, `readOnly` 네이티브
 * 속성으로 표현한다.
 *
 * 너비는 prop으로 두지 않는다. 기본 `w-full`로 부모 폭을 따른다.
 */
export interface InputProps extends Omit<ComponentProps<'input'>, 'size'> {
  /** @default 'filled' */
  variant?: InputVariant;
  /** @default 'md' */
  size?: InputSize;
}

const VARIANT_CLASSES: Record<InputVariant, string> = {
  filled: 'bg-gray-100',
  outlined: 'border border-gray-200',
};

const SIZE_CLASSES: Record<InputSize, string> = {
  sm: 'h-10',
  md: 'h-11',
  lg: 'h-12',
};

export function Input({ variant = 'filled', size = 'md', className, ...props }: InputProps) {
  return (
    <input
      className={cn(
        'w-full rounded-lg px-4 text-sm text-gray-900 outline-none',
        'placeholder:text-gray-500',
        'read-only:text-gray-600',
        'focus-visible:ring-primary-600 focus-visible:ring-2',
        'aria-invalid:ring-1 aria-invalid:ring-red-600',
        VARIANT_CLASSES[variant],
        SIZE_CLASSES[size],
        className,
      )}
      {...props}
    />
  );
}

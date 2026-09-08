import { useState, type ComponentProps } from 'react';

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
 * 항상 `<div>`를 렌더하고, 보여줄 이미지가 있을 때만 그 안에 `<img>`를 둔다.
 * `src`가 비어 있거나 로드에 실패하면 `<img>`를 렌더하지 않아 `primary-50` 배경의 빈 원만 남는다.
 */
export interface AvatarProps extends Omit<ComponentProps<'div'>, 'children'> {
  /** @default 'md' */
  size?: AvatarSize;
  src?: string;
  /**
   * 옆에 이름이 함께 노출되면 빈 문자열을 넘겨 중복 낭독을 막는다.
   * 아바타만 단독으로 쓰이면 이름을 넣는다.
   */
  alt: string;
  /** 내부 `<img>`에 전달되는 값 */
  loading?: ComponentProps<'img'>['loading'];
}

const BASE = 'shrink-0 overflow-hidden rounded-full bg-primary-50';

const SIZE_CLASSES: Record<AvatarSize, string> = {
  xs: 'size-5',
  sm: 'size-7',
  md: 'size-8',
  lg: 'size-13',
};

export function Avatar({ size = 'md', src, alt, loading, className, ...props }: AvatarProps) {
  const [failedSrc, setFailedSrc] = useState<string>();
  const showImage = Boolean(src) && src !== failedSrc;

  return (
    <div
      className={cn(BASE, SIZE_CLASSES[size], className)}
      role={!showImage && alt ? 'img' : undefined}
      aria-label={showImage ? undefined : alt || undefined}
      {...props}
    >
      {showImage && (
        <img
          src={src}
          alt={alt}
          loading={loading}
          onError={() => setFailedSrc(src)}
          className="size-full object-cover"
        />
      )}
    </div>
  );
}

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

interface AvatarBaseProps {
  /** @default 'md' */
  size?: AvatarSize;
  /**
   * 옆에 이름이 함께 노출되면 빈 문자열을 넘겨 중복 낭독을 막는다.
   * 아바타만 단독으로 쓰이면 사람 이름을 넣는다.
   */
  alt: string;
}

/** `src`가 있을 때. `<img>`로 렌더하므로 `loading` 같은 이미지 전용 속성을 받는다. */
export type AvatarImageProps = AvatarBaseProps &
  Omit<ComponentProps<'img'>, 'width' | 'height' | 'alt'> & { src: string };

/** `src`가 없을 때. `<div>`로 렌더하므로 이미지 전용 속성은 받지 않는다. */
export type AvatarFallbackProps = AvatarBaseProps &
  Omit<ComponentProps<'div'>, 'children'> & { src?: never };

/**
 * 원형 이미지 박스만 담당한다. 표시할 이미지는 호출부가 `src`로 넘긴다.
 *
 * `src`가 비어 있으면 `<img>` 대신 `<div>`로 렌더해 `primary-50` 배경의 빈 원만
 * 남긴다. `src`가 없거나 빈 문자열인 `<img>`는 브라우저가 깨진 이미지로 취급해
 * 아이콘과 `alt` 텍스트를 그려버리기 때문이다. 두 경우의 화면 결과가 같으므로
 * `undefined`와 `''`을 함께 처리한다.
 *
 * `width`와 `height`는 `size`와 충돌하므로 가린다.
 */
export type AvatarProps = AvatarImageProps | AvatarFallbackProps;

const BASE = 'shrink-0 rounded-full bg-primary-50 object-cover';

const SIZE_CLASSES: Record<AvatarSize, string> = {
  xs: 'size-5',
  sm: 'size-7',
  md: 'size-8',
  lg: 'size-13',
};

export function Avatar({ size = 'md', className, ...props }: AvatarProps) {
  const classes = cn(BASE, SIZE_CLASSES[size], className);

  if (!props.src) {
    // src는 `<div>`에 넘기지 않기 위해 분해만 하고 쓰지 않는다.
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { alt, src: _src, ...rest } = props;

    return (
      <div
        className={classes}
        role={alt ? 'img' : undefined}
        aria-label={alt || undefined}
        {...rest}
      />
    );
  }

  return <img className={classes} {...props} />;
}

import { useState, type ComponentProps } from 'react';
import { IconUser } from '@tabler/icons-react';
import { cn } from '@/utils/cn';
import { AVATAR_TONE_CLASSES, getAvatarInitial, getAvatarTone } from '@/utils/avatar';

/**
 * 지름 단계.
 *
 * - `xs`: 20px — 아바타만 표시하는 목록
 * - `sm`: 28px — 카드 작성자
 * - `md`: 32px — 헤더, 데스크톱 작성자
 * - `lg`: 52px — 프로필 헤더
 */
export type AvatarSize = 'xs' | 'sm' | 'md' | 'lg';

/**
 * 원형 이미지 박스.
 *
 * `src`가 있고 로드에 성공하면 이미지 렌더링
 * 아닌 경우, `name`의 첫 글자를 이름에서 뽑은 색 위에 보여줌(기본 프로필)
 *
 */
export interface AvatarProps extends Omit<ComponentProps<'div'>, 'children'> {
  /** @default 'md' */
  size?: AvatarSize;
  src?: string | null;
  name?: string;
  alt: string;
  anonymous?: boolean;
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

const INITIAL_SIZE_CLASSES: Record<AvatarSize, string> = {
  xs: 'text-[0.625rem]',
  sm: 'text-xs',
  md: 'text-sm',
  lg: 'text-xl',
};

const ANONYMOUS_ICON_SIZE: Record<AvatarSize, string> = {
  xs: 'size-2.5',
  sm: 'size-3.5',
  md: 'size-4',
  lg: 'size-6',
};

export function Avatar({
  size = 'md',
  src,
  name,
  alt,
  anonymous = false,
  loading,
  className,
  ...props
}: AvatarProps) {
  const [failedSrc, setFailedSrc] = useState<string>();

  const imageSrc = !anonymous && src && src !== failedSrc ? src : undefined;
  const showImage = imageSrc !== undefined;
  const initial = !anonymous && name ? getAvatarInitial(name) : '';
  const showInitial = !showImage && initial !== '';

  const imageRole = !showImage && alt ? ({ role: 'img', 'aria-label': alt } as const) : undefined;

  return (
    <div
      className={cn(
        BASE,
        SIZE_CLASSES[size],
        anonymous && 'flex items-center justify-center bg-gray-100 text-gray-400',
        showInitial && [
          'flex items-center justify-center font-bold',
          INITIAL_SIZE_CLASSES[size],
          AVATAR_TONE_CLASSES[getAvatarTone(name ?? '')],
        ],
        className,
      )}
      {...imageRole}
      {...props}
    >
      {showImage && (
        <img
          src={imageSrc}
          alt={alt}
          loading={loading}
          onError={() => setFailedSrc(imageSrc)}
          className="size-full object-cover"
        />
      )}
      {showInitial && <span aria-hidden="true">{initial}</span>}
      {anonymous && <IconUser className={ANONYMOUS_ICON_SIZE[size]} aria-hidden="true" />}
    </div>
  );
}

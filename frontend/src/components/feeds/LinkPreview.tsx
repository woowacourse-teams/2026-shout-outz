import { IconLink } from '@tabler/icons-react';
import { Image } from '@/components/Image';
import { cn } from '@/utils/cn';

export interface LinkPreviewProps {
  url: string;
  title?: string | null;
  description?: string | null;
  imageUrl?: string | null;
  siteName?: string | null;
  onClick?: () => void;
  /** sm은 댓글처럼 좁은 곳에 둔다. 썸네일을 줄이고 설명은 뺀다. */
  size?: 'sm' | 'md';
}

/**
 * 왼쪽 썸네일, 오른쪽 글자로 놓는 가로형 링크 카드.
 *
 * 썸네일은 OG 이미지 표준 비율(1200×630 ≈ 1.91:1)로 높이만 고정해 이미지가 거의 잘리지 않는다.
 * 이미지가 없으면 정사각형 아이콘 칸으로 줄여 글자 쪽에 폭을 더 준다.
 */
export function LinkPreview({
  url,
  title,
  description,
  imageUrl,
  siteName,
  size = 'md',
  onClick,
}: LinkPreviewProps) {
  const small = size === 'sm';
  const imageFallback = (
    <div className="flex size-full items-center justify-center">
      <IconLink className={cn('text-gray-400', small ? 'size-4' : 'size-6')} aria-hidden="true" />
    </div>
  );

  return (
    <a
      href={url}
      target="_blank"
      rel="noopener noreferrer"
      onClick={onClick}
      aria-label={`${title || siteName || url} 링크 열기`}
      className="flex overflow-hidden rounded-lg bg-gray-50"
    >
      <div
        className={cn(
          'shrink-0 bg-gray-100',
          small ? 'h-14' : 'h-20 md:h-28',
          imageUrl ? 'aspect-[1.91/1]' : 'aspect-square',
        )}
      >
        {imageUrl ? (
          <Image
            src={imageUrl}
            alt=""
            loading="lazy"
            className="size-full"
            fallback={imageFallback}
          />
        ) : (
          imageFallback
        )}
      </div>
      <div
        className={cn(
          'flex min-w-0 flex-1 flex-col justify-center',
          small ? 'gap-0.5 px-3 py-1.5' : 'gap-1 px-3 py-2 md:px-4 md:py-3',
        )}
      >
        {title && (
          <p
            className={cn(
              'line-clamp-1 font-semibold text-gray-800',
              small ? 'text-xs' : 'text-sm',
            )}
          >
            {title}
          </p>
        )}
        {!small && description && (
          <p className="line-clamp-1 text-xs text-gray-600 md:line-clamp-2">{description}</p>
        )}
        <p className="truncate text-xs text-gray-500">{siteName || url}</p>
      </div>
    </a>
  );
}

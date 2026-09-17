import { IconLink } from '@tabler/icons-react';
import { Image } from '@/components/Image';

export interface LinkPreviewProps {
  url: string;
  title?: string | null;
  description?: string | null;
  imageUrl?: string | null;
  siteName?: string | null;
}

export function LinkPreview({ url, title, description, imageUrl, siteName }: LinkPreviewProps) {
  const titleFallback = (
    <div className="flex h-20 items-center px-3 md:h-28 md:px-4">
      <p className="line-clamp-2 text-sm font-semibold text-gray-500">{title}</p>
    </div>
  );
  const imageFallback = (
    <div className="flex h-20 items-center justify-center md:h-28">
      <IconLink className="size-6 text-gray-400" aria-hidden="true" />
    </div>
  );

  return (
    <a
      href={url}
      target="_blank"
      rel="noopener noreferrer"
      aria-label={`${title || siteName || url} 링크 열기`}
      className="block overflow-hidden rounded-lg bg-gray-50"
    >
      <div className="bg-gray-100">
        {imageUrl ? (
          <Image
            src={imageUrl}
            alt=""
            loading="lazy"
            className="h-20 w-full md:h-28"
            fallback={imageFallback}
          />
        ) : title ? (
          titleFallback
        ) : (
          imageFallback
        )}
      </div>
      <div className="space-y-1 px-3 py-2 md:px-4 md:py-3">
        {imageUrl && title && (
          <p className="line-clamp-2 text-sm font-semibold text-gray-800">{title}</p>
        )}
        {description && <p className="line-clamp-2 text-xs text-gray-600">{description}</p>}
        <p className="truncate text-xs text-gray-500">{siteName || url}</p>
      </div>
    </a>
  );
}

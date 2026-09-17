import { useState, type ComponentProps, type ReactNode } from 'react';

import { cn } from '@/utils/cn';

export interface ImageProps extends ComponentProps<'img'> {
  fallback?: ReactNode;
}

export function Image({ src, alt, fallback = null, className, onError, ...props }: ImageProps) {
  const [failedSrc, setFailedSrc] = useState<string>();

  if (!src || src === failedSrc) return fallback;

  return (
    <img
      {...props}
      src={src}
      alt={alt}
      className={cn('object-cover', className)}
      onError={(event) => {
        setFailedSrc(src);
        onError?.(event);
      }}
    />
  );
}

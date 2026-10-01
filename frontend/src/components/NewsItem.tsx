import type { ComponentProps } from 'react';

import { NewsCategoryBadge } from '@/components/NewsCategoryBadge';
import { cn } from '@/utils/cn';
import { type NewsType } from '@/types/news';
import { formatDotDate } from '@/utils/date';

export interface NewsItemProps extends Omit<ComponentProps<'article'>, 'children'> {
  type: NewsType;
  title: string;
  summary: string;
  publishedAt: string;
  compact?: boolean;
}

export function NewsItem({
  type,
  title,
  summary,
  publishedAt,
  compact = false,
  className,
  ...props
}: NewsItemProps) {
  return (
    <article
      className={cn('flex min-w-0 flex-col gap-3', compact && 'gap-2', className)}
      {...props}
    >
      <div className="flex items-center justify-between gap-3">
        <NewsCategoryBadge type={type} />
        <time dateTime={publishedAt} className="shrink-0 text-xs text-gray-500">
          {formatDotDate(publishedAt)}
        </time>
      </div>

      <h2
        className={cn(
          'group-hover:text-primary-600 leading-6 font-semibold tracking-tight break-words text-gray-900',
          compact ? 'line-clamp-2 text-sm' : 'text-base',
        )}
      >
        {title}
      </h2>

      <p
        className={cn(
          'line-clamp-2 break-words text-gray-600',
          compact ? 'text-xs leading-5' : 'text-sm leading-6',
        )}
      >
        {summary}
      </p>
    </article>
  );
}

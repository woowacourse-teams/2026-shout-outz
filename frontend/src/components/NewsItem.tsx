import type { ComponentProps } from 'react';

import { NewsEventStatusBadge } from '@/components/NewsEventStatusBadge';
import { NewsEventPeriod } from '@/components/NewsEventPeriod';
import type { NewsSummary } from '@/types/news';
import { NewsCategoryBadge } from '@/components/NewsCategoryBadge';
import { cn } from '@/utils/cn';
import { type NewsType } from '@/types/news';
import { formatDotDate } from '@/utils/date';

export interface NewsItemProps extends Omit<ComponentProps<'article'>, 'children'> {
  type: NewsType;
  title: string;
  summary: string;
  publishedAt: string;
  eventStatus?: NewsSummary['eventStatus'];
  eventStartAt?: NewsSummary['eventStartAt'];
  eventEndAt?: NewsSummary['eventEndAt'];
  compact?: boolean;
}

export function NewsItem({
  type,
  title,
  summary,
  publishedAt,
  eventStatus,
  eventStartAt,
  eventEndAt,
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
        <div className="flex flex-wrap items-center gap-2">
          {type === 'EVENT' && <NewsEventStatusBadge status={eventStatus} />}
          <NewsCategoryBadge type={type} />
        </div>
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
      {type === 'EVENT' && <NewsEventPeriod startAt={eventStartAt} endAt={eventEndAt} compact />}
    </article>
  );
}

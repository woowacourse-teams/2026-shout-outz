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
    <article className={cn('flex min-w-0 flex-col gap-2', className)} {...props}>
      <div className="flex items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          {type === 'EVENT' && (
            <NewsEventStatusBadge status={eventStatus} variant={compact ? 'solid' : 'soft'} />
          )}
          <NewsCategoryBadge type={type} />
        </div>
        <time
          dateTime={publishedAt}
          className={cn('shrink-0 text-xs', compact ? 'text-gray-500' : 'text-gray-400')}
        >
          {formatDotDate(publishedAt)}
        </time>
      </div>

      <h2
        className={cn(
          'tracking-tight break-words text-gray-900',
          compact
            ? 'group-hover:text-primary-600 line-clamp-2 text-sm leading-6 font-semibold'
            : 'text-lg leading-snug font-bold group-hover:underline',
        )}
      >
        {title}
      </h2>

      <p
        className={cn(
          'line-clamp-2 break-words text-gray-600',
          compact ? 'text-xs leading-5' : 'text-sm leading-relaxed md:text-base',
        )}
      >
        {summary}
      </p>
      {type === 'EVENT' && (
        <NewsEventPeriod startAt={eventStartAt} endAt={eventEndAt} compact strongDates={!compact} />
      )}
    </article>
  );
}

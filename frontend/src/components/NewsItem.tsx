import type { ComponentProps } from 'react';

import { NewsCategoryBadge } from '@/components/NewsCategoryBadge';
import { cn } from '@/utils/cn';
import type { NewsType } from '@/types/news';
import { formatDotDate } from '@/utils/date';

export interface NewsItemProps extends Omit<ComponentProps<'article'>, 'children'> {
  type: NewsType;
  title: string;
  summary: string;
  publishedAt: string;
}

export function NewsItem({
  type,
  title,
  summary,
  publishedAt,
  className,
  ...props
}: NewsItemProps) {
  return (
    <article className={cn('flex flex-col gap-1.5 md:gap-2.5', className)} {...props}>
      <div className="flex items-center gap-1.5 md:gap-2">
        <NewsCategoryBadge type={type} />
        <time dateTime={publishedAt} className="text-xs text-gray-500">
          {formatDotDate(publishedAt)}
        </time>
      </div>

      <h2 className="text-sm leading-snug font-bold text-gray-900 md:text-base">{title}</h2>

      <p className="text-xs leading-normal text-gray-600 md:text-sm">{summary}</p>
    </article>
  );
}

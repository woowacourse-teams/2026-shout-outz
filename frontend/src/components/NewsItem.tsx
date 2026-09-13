import type { ComponentProps } from 'react';

import { Badge, type BadgeTone } from '@/components/Badge';
import { cn } from '@/utils/cn';
import { formatDotDate } from '@/utils/date';

export type NewsType = 'NOTICE' | 'EVENT';

export interface NewsItemProps extends Omit<ComponentProps<'article'>, 'children'> {
  type: NewsType;
  title: string;
  summary: string;
  publishedAt: string;
}

const TYPE_LABEL: Record<NewsType, string> = {
  NOTICE: '공지사항',
  EVENT: '이벤트',
};

const TYPE_TONE: Record<NewsType, BadgeTone> = {
  NOTICE: 'primary',
  EVENT: 'green',
};

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
        <Badge variant="solid" tone={TYPE_TONE[type]}>
          {TYPE_LABEL[type]}
        </Badge>
        <time dateTime={publishedAt} className="text-xs text-gray-500">
          {formatDotDate(publishedAt)}
        </time>
      </div>

      <h2 className="text-sm leading-snug font-bold text-gray-900 md:text-base">{title}</h2>

      <p className="text-xs leading-normal text-gray-600 md:text-sm">{summary}</p>
    </article>
  );
}

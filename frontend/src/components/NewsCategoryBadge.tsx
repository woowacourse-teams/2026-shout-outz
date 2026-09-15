import type { ComponentProps } from 'react';
import { Badge, type BadgeTone } from '@/components/Badge';
import { type NewsType } from '@/types/news';

const TYPE_LABEL: Record<NewsType, string> = {
  NOTICE: '공지사항',
  EVENT: '이벤트',
};

const TYPE_TONE: Record<NewsType, BadgeTone> = {
  NOTICE: 'primary',
  EVENT: 'green',
};

export interface NewsCategoryBadgeProps extends Omit<ComponentProps<'span'>, 'children'> {
  type: NewsType;
}

export function NewsCategoryBadge({ type, ...props }: NewsCategoryBadgeProps) {
  return (
    <Badge variant="solid" tone={TYPE_TONE[type]} {...props}>
      {TYPE_LABEL[type]}
    </Badge>
  );
}

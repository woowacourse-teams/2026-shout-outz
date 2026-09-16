import type { ComponentProps } from 'react';

import { Avatar } from '@/components/Avatar';
import { MarkdownContent } from '@/components/MarkdownContent';
import { type FeedAuthor } from '@/types/feed';
import { cn } from '@/utils/cn';
import { formatRelativeTime } from '@/utils/date';
import { formatAuthorLabel } from '@/utils/feed';

// TODO 좋아요·댓글 수와 OG 링크 카드는 응답에 데이터가 없어 아직 받지 않는다.
// TODO 피드 페이지 구현 PR develop 브랜치에 업데이트시 병합
export interface FeedCardProps extends Omit<ComponentProps<'article'>, 'children'> {
  author: FeedAuthor;
  content: string;
  createdAt: string;
}

export function FeedCard({ author, content, createdAt, className, ...props }: FeedCardProps) {
  return (
    <article className={cn('flex flex-col gap-3', className)} {...props}>
      <div className="flex items-center gap-2.5">
        {/* TODO avatarImageId를 미디어 API로 이미지 URL로 바꿔 src에 연결 */}
        <Avatar alt="" />
        <div className="flex min-w-0 flex-col gap-0.5">
          <p className="truncate text-sm font-bold text-gray-900">{formatAuthorLabel(author)}</p>
          <time dateTime={createdAt} className="text-xs text-gray-500">
            {formatRelativeTime(createdAt)}
          </time>
        </div>
      </div>

      <MarkdownContent>{content}</MarkdownContent>
    </article>
  );
}

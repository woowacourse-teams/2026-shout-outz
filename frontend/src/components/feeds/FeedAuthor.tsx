import type { Feed } from '@/apis/feed';
import { Avatar } from '@/components/Avatar';
import type { ReactNode } from 'react';

const TRACK_LABELS: Record<string, string> = {
  BACKEND: '백엔드',
  FRONTEND: '프론트엔드',
};

export function FeedAuthor({
  feed,
  compact = false,
  detail,
}: {
  feed: Feed;
  compact?: boolean;
  detail?: ReactNode;
}) {
  const { author } = feed;
  const track = author.track ? (TRACK_LABELS[author.track] ?? author.track) : null;
  const description = [author.cohort && `${author.cohort}기`, track].filter(Boolean).join(' ');

  return (
    <div className="flex min-w-0 items-center gap-2">
      <Avatar size={compact ? 'sm' : 'md'} alt={`${author.displayName} 프로필`} />
      <div className="min-w-0">
        <p className="truncate text-sm font-semibold text-gray-900">
          {author.displayName}
          {description && ` · ${description}`}
        </p>
        {detail}
      </div>
    </div>
  );
}

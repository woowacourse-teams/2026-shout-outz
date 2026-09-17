import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { IconHeart, IconMessageCircle, IconShare } from '@tabler/icons-react';
import type { Feed } from '@/apis/feed';
import { Avatar } from '@/components/Avatar';
import { Button } from '@/components/Button';
import { FeedContent } from '@/components/feeds/FeedContent';
import { FeedMenu } from '@/components/feeds/FeedMenu';
import { AsyncBoundary } from '@/components/feeds/AsyncBoundary';
import { Comments } from '@/components/feed-comments/Comments';
import { formatCrewName } from '@/utils/user';
import { formatRelativeTime } from '@/utils/date';

export function FeedCard({ feed }: { feed: Feed }) {
  const [open, setOpen] = useState(false);
  return (
    <article className="min-w-0 border-b border-gray-100 py-6 first:pt-4 md:py-7">
      <header className="flex items-start gap-3">
        <div className="min-w-0 flex-1">
          <Link to="/feeds/$feedId" params={{ feedId: String(feed.feedId) }}>
            <div className="flex min-w-0 items-center gap-2">
              <Avatar size="md" alt={`${feed.author.displayName} 프로필`} />
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-gray-900">
                  {formatCrewName(feed.author.displayName, feed.author.cohort, feed.author.track)}
                </p>
                <time className="text-sm text-gray-400" dateTime={feed.createdAt}>
                  {formatRelativeTime(feed.createdAt)}
                </time>
              </div>
            </div>
          </Link>
        </div>
        <AsyncBoundary>
          <FeedMenu feedId={feed.feedId} authorHandle={feed.author.handle} />
        </AsyncBoundary>
      </header>
      <FeedContent feed={feed} />
      <div className="mt-5 flex items-center gap-2 text-sm text-gray-500">
        <Button variant="ghost" size="sm" className="gap-1 px-2" aria-label="좋아요" disabled>
          <IconHeart className="size-4" aria-hidden="true" />
        </Button>
        <Button
          variant="ghost"
          size="sm"
          className="gap-1 px-2"
          aria-label="댓글"
          aria-expanded={open}
          aria-controls={`comments-${feed.feedId}`}
          onClick={() => setOpen(!open)}
        >
          <IconMessageCircle className="size-4" aria-hidden="true" />
        </Button>
        <span className="ml-auto">
          <Button
            variant="ghost"
            size="sm"
            className="px-2"
            aria-label="공유"
            onClick={() =>
              void navigator.clipboard?.writeText(
                new URL(`/feeds/${feed.feedId}`, window.location.origin).href,
              )
            }
          >
            <IconShare className="size-4" aria-hidden="true" />
          </Button>
        </span>
      </div>
      {open && (
        <section
          id={`comments-${feed.feedId}`}
          aria-label={`${feed.author.displayName} 피드 댓글`}
          className="mt-5 border-t border-gray-100 pt-5"
        >
          <Comments feedId={feed.feedId} />
        </section>
      )}
    </article>
  );
}

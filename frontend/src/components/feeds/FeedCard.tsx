import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import type { Feed } from '@/apis/feed';
import { Avatar } from '@/components/Avatar';
import { Button } from '@/components/Button';
import { FeedContent } from '@/components/feeds/FeedContent';
import { Comments } from '@/components/feed-comments/Comments';
import { getFeedAuthorName } from '@/utils/feed';
import { formatRelativeTime } from '@/utils/date';

export function FeedCard({ feed }: { feed: Feed }) {
  const [open, setOpen] = useState(false);
  return (
    <article className="min-w-0 border-b border-gray-100 py-6 first:pt-4 md:py-7">
      <header className="flex items-start gap-3">
        <div className="min-w-0 flex-1">
          <Link to="/feeds/$postId" params={{ postId: String(feed.postId) }}>
            <div className="flex min-w-0 items-center gap-2">
              <Avatar size="md" alt={`${feed.author.displayName} 프로필`} />
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-gray-900">
                  {getFeedAuthorName(feed.author)}
                </p>
                <time className="text-sm text-gray-400" dateTime={feed.createdAt}>
                  {formatRelativeTime(feed.createdAt)}
                </time>
              </div>
            </div>
          </Link>
        </div>
        <Button variant="ghost" size="sm" aria-label="피드 메뉴">
          ···
        </Button>
      </header>
      <FeedContent feed={feed} />
      <div className="mt-5 flex items-center gap-2 text-sm text-gray-500">
        <Button variant="ghost" size="sm" onClick={() => {}}>
          좋아요
        </Button>
        <Button
          variant="ghost"
          size="sm"
          aria-expanded={open}
          aria-controls={`comments-${feed.postId}`}
          onClick={() => setOpen(!open)}
        >
          댓글
        </Button>
        <span className="ml-auto">
          <Button
            variant="ghost"
            size="sm"
            onClick={() =>
              void navigator.clipboard?.writeText(
                new URL(`/feeds/${feed.postId}`, window.location.origin).href,
              )
            }
          >
            공유
          </Button>
        </span>
      </div>
      {open && (
        <section
          id={`comments-${feed.postId}`}
          aria-label={`${feed.author.displayName} 피드 댓글`}
          className="mt-5 border-t border-gray-100 pt-5"
        >
          <Comments postId={feed.postId} />
        </section>
      )}
    </article>
  );
}

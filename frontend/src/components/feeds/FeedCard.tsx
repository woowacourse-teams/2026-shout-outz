import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { IconMessageCircle } from '@tabler/icons-react';
import type { Feed } from '@/apis/feed';
import { Button } from '@/components/Button';
import { FeedAuthor } from '@/components/feeds/FeedAuthor';
import { FeedLikeButton } from '@/components/feeds/FeedLikeButton';
import { FeedContent } from '@/components/feeds/FeedContent';
import { FeedMenu } from '@/components/feeds/FeedMenu';
import { ShareButton } from '@/components/feeds/ShareButton';
import { AsyncBoundary } from '@/components/feeds/AsyncBoundary';
import { Comments } from '@/components/feed-comments/Comments';
import { analytics, type FeedSurface } from '@/utils/analytics';

export function FeedCard({ feed, surface }: { feed: Feed; surface: FeedSurface }) {
  const [open, setOpen] = useState(false);
  return (
    <article className="min-w-0 border-b border-gray-100 py-6 first:pt-4 md:py-7">
      <div className="flex items-start gap-3">
        <div className="min-w-0 flex-1">
          <Link
            to="/feeds/$feedId"
            params={{ feedId: String(feed.feedId) }}
            onClick={() =>
              analytics.track({ name: 'feed_detail_opened', feedId: feed.feedId, from: surface })
            }
          >
            <FeedAuthor author={feed.author} createdAt={feed.createdAt} />
          </Link>
        </div>
        <AsyncBoundary>
          <FeedMenu feedId={feed.feedId} authorHandle={feed.author.handle} />
        </AsyncBoundary>
      </div>
      <FeedContent feed={feed} />
      <div className="mt-5 flex items-center gap-2 text-sm text-gray-500">
        <FeedLikeButton
          feedId={feed.feedId}
          likeCount={feed.likeCount}
          likedByMe={feed.likedByMe}
        />
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
          <span aria-label="댓글 수">{feed.commentCount ?? 0}</span>
        </Button>
        <span className="ml-auto">
          <ShareButton
            url={new URL(`/feeds/${feed.feedId}`, window.location.origin).href}
            className="px-2"
          />
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

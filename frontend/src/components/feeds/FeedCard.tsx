import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { IconMessageCircle } from '@tabler/icons-react';
import type { Feed } from '@/apis/feed';
import { Button } from '@/components/Button';
import { Image } from '@/components/Image';
import { FeedAuthor } from '@/components/feeds/FeedAuthor';
import { FeedLikeButton } from '@/components/feeds/FeedLikeButton';
import { FeedMarkdown } from '@/components/feeds/FeedMarkdown';
import { LinkPreview } from '@/components/feeds/LinkPreview';
import { FeedMenu } from '@/components/feeds/FeedMenu';
import { ShareButton } from '@/components/feeds/ShareButton';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Comments } from '@/components/feed-comments/Comments';
import { analytics, type FeedSurface } from '@/utils/analytics';

export function FeedCard({ feed, surface }: { feed: Feed; surface: FeedSurface }) {
  const [open, setOpen] = useState(false);
  const [firstMedia, ...restMedia] = [...feed.media].sort(
    (a, b) => a.displayOrder - b.displayOrder,
  );
  const linkPreview = feed.linkPreview;

  return (
    <article className="min-w-0 border-b border-gray-100 py-6 first:pt-4 md:py-7">
      <div className="flex items-start gap-3">
        <div className="min-w-0 flex-1">
          <FeedAuthor author={feed.author} createdAt={feed.createdAt} />
        </div>
        <AsyncBoundary>
          <FeedMenu feedId={feed.feedId} authorHandle={feed.author.handle} />
        </AsyncBoundary>
      </div>
      <div className="relative">
        <h3 className="mt-4 text-base leading-snug font-bold break-words text-gray-900 md:text-lg">
          <Link
            to="/feeds/$feedId"
            params={{ feedId: String(feed.feedId) }}
            onClick={() =>
              analytics.track({ name: 'feed_detail_opened', feedId: feed.feedId, from: surface })
            }
            className="focus-visible:outline-primary-600 after:absolute after:inset-0 focus-visible:outline-2"
          >
            {feed.title}
          </Link>
        </h3>
        <div className="mt-2 line-clamp-5 space-y-3 text-base leading-7 break-words text-gray-800">
          <FeedMarkdown content={feed.content} hideCodeBlocks />
        </div>
        {linkPreview?.url && (
          <div className="relative z-10 mt-4">
            <LinkPreview {...linkPreview} url={linkPreview.url} />
          </div>
        )}
        {firstMedia && (
          <div className="pointer-events-none relative mt-4">
            <Image
              src={firstMedia.url}
              alt="피드 첨부 이미지"
              loading="lazy"
              className="max-h-96 w-full rounded-xl bg-gray-50 object-contain"
              fallback={<p className="text-sm text-gray-500">이미지를 불러오지 못했습니다.</p>}
            />
            {restMedia.length > 0 && (
              <span className="absolute right-3 bottom-3 rounded-full bg-gray-900/70 px-2.5 py-1 text-xs font-bold text-white">
                <span className="sr-only">이미지 </span>+{restMedia.length}
                <span className="sr-only">장 더 있음</span>
              </span>
            )}
          </div>
        )}
      </div>
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

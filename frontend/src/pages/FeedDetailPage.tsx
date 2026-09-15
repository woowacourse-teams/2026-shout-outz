import { useSuspenseQuery } from '@tanstack/react-query';
import { feedQuery } from '@/apis/feed';
import { Avatar } from '@/components/Avatar';
import { Gnb } from '@/components/Gnb';
import { Button } from '@/components/Button';
import { AsyncBoundary } from '@/components/feeds/AsyncBoundary';
import { FeedContent } from '@/components/feeds/FeedContent';
import { FeedFooter } from '@/components/feeds/FeedFooter';
import { PopularFeedList } from '@/components/feeds/PopularFeedList';
import { Comments } from '@/components/feed-comments/Comments';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { getFeedAuthorName } from '@/utils/feed';
import { formatRelativeTime } from '@/utils/date';

const DESKTOP_MEDIA_QUERY = '(min-width: 64rem)';

export function FeedDetailPage({ postId }: { postId: number }) {
  const isDesktop = useMediaQuery(DESKTOP_MEDIA_QUERY);

  return (
    <div className="bg-background flex min-h-dvh flex-col">
      <Gnb />
      <main className="mx-auto grid w-full max-w-6xl flex-1 grid-cols-1 px-4 py-6 md:px-16 md:py-10 lg:grid-cols-3 lg:gap-12">
        <section className="min-w-0 lg:col-span-2" aria-label="피드 상세">
          <AsyncBoundary key={postId}>
            <FeedDetailContent postId={postId} />
          </AsyncBoundary>
        </section>
        {isDesktop && (
          <div className="min-w-0">
            <AsyncBoundary>
              <PopularFeedList />
            </AsyncBoundary>
          </div>
        )}
      </main>
      <FeedFooter />
    </div>
  );
}

function FeedDetailContent({ postId }: { postId: number }) {
  const { data: feed } = useSuspenseQuery(feedQuery(postId));

  return (
    <article className="min-w-0">
      <title>{`${feed.author.displayName}의 피드 | shout-outz`}</title>
      <div className="flex min-w-0 items-center gap-2">
        <Avatar size="md" alt={`${feed.author.displayName} 프로필`} />
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-gray-900">
            {getFeedAuthorName(feed.author)}
          </p>
          <time dateTime={feed.createdAt} className="text-sm text-gray-500">
            {formatRelativeTime(feed.createdAt)}
          </time>
        </div>
      </div>
      <FeedContent feed={feed} />
      <div className="mt-5 flex items-center gap-2">
        <Button variant="ghost" size="sm" disabled>
          좋아요
        </Button>
        <Button
          variant="ghost"
          size="sm"
          onClick={() => document.getElementById('feed-comments')?.scrollIntoView()}
        >
          댓글
        </Button>
        <Button
          variant="ghost"
          size="sm"
          className="ml-auto"
          onClick={() => void navigator.clipboard?.writeText(window.location.href)}
        >
          공유
        </Button>
      </div>
      <section id="feed-comments" aria-label="피드 댓글" className="mt-5">
        <Comments postId={feed.postId} />
      </section>
    </article>
  );
}

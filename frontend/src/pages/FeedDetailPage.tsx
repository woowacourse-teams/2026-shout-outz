import { useSuspenseQuery } from '@tanstack/react-query';
import { IconMessageCircle } from '@tabler/icons-react';
import { feedQuery } from '@/apis/feed';
import { Button } from '@/components/Button';
import { Footer } from '@/components/Footer';
import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/feeds/AsyncBoundary';
import { FeedContent } from '@/components/feeds/FeedContent';
import { FeedAuthor } from '@/components/feeds/FeedAuthor';
import { FeedLikeButton } from '@/components/feeds/FeedLikeButton';
import { FeedMenu } from '@/components/feeds/FeedMenu';
import { ShareButton } from '@/components/feeds/ShareButton';
import { PopularFeedList } from '@/components/feeds/PopularFeedList';
import { Comments } from '@/components/feed-comments/Comments';
import { useMediaQuery } from '@/hooks/useMediaQuery';

const DESKTOP_MEDIA_QUERY = '(min-width: 64rem)';

export function FeedDetailPage({ feedId }: { feedId: number }) {
  const isDesktop = useMediaQuery(DESKTOP_MEDIA_QUERY);

  return (
    <div className="bg-background flex min-h-dvh flex-col">
      <AppGnb />
      <main className="mx-auto grid w-full max-w-6xl flex-1 grid-cols-1 px-4 py-6 md:py-10 lg:grid-cols-3 lg:gap-12">
        <section className="min-w-0 lg:col-span-2" aria-label="피드 상세">
          <AsyncBoundary key={feedId}>
            <FeedDetailContent feedId={feedId} />
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
      <Footer />
    </div>
  );
}

function FeedDetailContent({ feedId }: { feedId: number }) {
  const { data: feed } = useSuspenseQuery(feedQuery(feedId));

  return (
    <article className="min-w-0">
      <title>{`${feed.author.displayName}의 피드 | shout-outz`}</title>
      <div className="flex items-start justify-between gap-3">
        <FeedAuthor author={feed.author} createdAt={feed.createdAt} />
        <AsyncBoundary>
          <FeedMenu feedId={feed.feedId} authorHandle={feed.author.handle} />
        </AsyncBoundary>
      </div>
      <FeedContent feed={feed} titleAs="h2" />
      <div className="mt-5 flex items-center gap-2">
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
          onClick={() => document.getElementById('feed-comments')?.scrollIntoView()}
        >
          <IconMessageCircle className="size-4" aria-hidden="true" />
          <span aria-label="댓글 수">{feed.commentCount ?? 0}</span>
        </Button>
        <ShareButton url={window.location.href} className="ml-auto px-2" />
      </div>
      <section id="feed-comments" aria-label="피드 댓글" className="mt-5">
        <Comments feedId={feed.feedId} />
      </section>
    </article>
  );
}

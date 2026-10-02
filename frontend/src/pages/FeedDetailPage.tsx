import { useSuspenseQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { IconArrowLeft, IconMessageCircle } from '@tabler/icons-react';
import { feedQuery } from '@/apis/feed';
import { Button } from '@/components/Button';
import { Footer } from '@/components/Footer';
import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { FeedDetailBody } from '@/components/feeds/FeedDetailBody';
import { FeedAuthor } from '@/components/feeds/FeedAuthor';
import { FeedLikeButton } from '@/components/feeds/FeedLikeButton';
import { FeedMenu } from '@/components/feeds/FeedMenu';
import { ShareButton } from '@/components/feeds/ShareButton';
import { Comments } from '@/components/feed-comments/Comments';
import { formatDotDate } from '@/utils/date';

export function FeedDetailPage({ feedId }: { feedId: number }) {
  return (
    <div className="bg-background flex min-h-dvh flex-col">
      <AppGnb />
      <main className="mx-auto w-full max-w-4xl flex-1 px-5 pt-7 pb-16 md:px-12 md:pt-8 md:pb-20">
        <section className="min-w-0" aria-label="커뮤니티 글 상세">
          <AsyncBoundary key={feedId}>
            <FeedDetailContent feedId={feedId} />
          </AsyncBoundary>
        </section>
      </main>
      <Footer />
    </div>
  );
}

function FeedDetailContent({ feedId }: { feedId: number }) {
  const { data: feed } = useSuspenseQuery(feedQuery(feedId));
  const isQuestion = feed.feedType === 'QUESTION';
  const commentLabel = isQuestion ? '답변' : '댓글';

  return (
    <article className="min-w-0">
      <title>{`${feed.title} | shout-outz`}</title>
      <Link
        to="/community"
        search={{ sort: 'LATEST', type: isQuestion ? 'QUESTION' : 'POST' }}
        className="mb-7 inline-flex items-center gap-2 text-xs text-gray-500 hover:text-gray-900"
      >
        <IconArrowLeft className="size-4" aria-hidden="true" />
        목록으로
      </Link>
      <FeedDetailBody
        feed={feed}
        author={
          <div className="relative flex items-center">
            <FeedAuthor author={feed.author} isAnonymous={feed.isAnonymous} />
            <div className="absolute top-1/2 right-0 z-10 flex -translate-y-1/2 items-center gap-2">
              <ShareButton url={window.location.href} />
              {feed.author.handle && (
                <AsyncBoundary>
                  <FeedMenu
                    feedId={feed.feedId}
                    authorHandle={feed.author.handle}
                    feedType={isQuestion ? 'QUESTION' : 'POST'}
                  />
                </AsyncBoundary>
              )}
            </div>
          </div>
        }
      />
      <div className="mt-6 flex flex-wrap items-center gap-2 border-b border-gray-100 pb-5">
        <FeedLikeButton
          feedId={feed.feedId}
          likeCount={feed.likeCount}
          likedByMe={feed.likedByMe}
          label={isQuestion ? '저도 궁금해요' : '좋아요'}
        />
        <Button
          variant="ghost"
          size="sm"
          className="gap-1 px-2"
          aria-label={commentLabel}
          onClick={() => document.getElementById('feed-comments')?.scrollIntoView()}
        >
          <IconMessageCircle className="size-4" aria-hidden="true" />
          <span aria-label={`${commentLabel} 수`}>{feed.commentCount ?? 0}</span>
        </Button>
        <time dateTime={feed.createdAt} className="ml-auto text-xs text-gray-500">
          {formatDotDate(feed.createdAt)}
        </time>
      </div>
      <section
        id="feed-comments"
        aria-label={`${commentLabel} 목록`}
        className="mt-12 px-0 md:px-3"
      >
        <Comments
          feedId={feed.feedId}
          feedType={isQuestion ? 'QUESTION' : 'POST'}
          feedAuthorId={feed.author.userId ?? null}
        />
      </section>
    </article>
  );
}

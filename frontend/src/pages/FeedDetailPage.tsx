import { useEffect } from 'react';
import { useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { IconArrowLeft, IconMessageCircle } from '@tabler/icons-react';
import { feedQuery } from '@/apis/feed';
import { Button } from '@/components/Button';
import { Footer } from '@/components/Footer';
import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { ClientOnly } from '@/components/ClientOnly';
import { FeedDetailBody } from '@/components/feeds/FeedDetailBody';
import { FeedAuthor } from '@/components/feeds/FeedAuthor';
import { FeedLikeButton } from '@/components/feeds/FeedLikeButton';
import { FeedMenu } from '@/components/feeds/FeedMenu';
import { ShareButton } from '@/components/feeds/ShareButton';
import { Comments } from '@/components/feed-comments/Comments';
import { RelatedQuestions } from '@/components/recommendations/RelatedQuestions';
import { useSession } from '@/hooks/useSession';
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
  const queryClient = useQueryClient();
  const { data: session } = useSession();
  const isAuthenticated = session?.status === 'AUTHENTICATED';

  // SSR 서버는 쿠키 없이 글을 받아 likedByMe가 비로그인 기준입니다. 브라우저에서 로그인이 확인되면
  // 쿠키를 붙여 다시 받아, 빈 하트로 그려진 좋아요가 내 상태로 바뀌게 합니다.
  useEffect(() => {
    if (!isAuthenticated) return;
    void queryClient.invalidateQueries({ queryKey: feedQuery(feedId).queryKey, exact: true });
  }, [isAuthenticated, feedId, queryClient]);

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
          <div className="relative flex items-center pr-24 sm:pr-64">
            <FeedAuthor author={feed.author} isAnonymous={feed.isAnonymous} />
            <div className="absolute top-1/2 right-0 z-10 flex -translate-y-1/2 items-center gap-2">
              <ShareButton />
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
      {isQuestion &&
        feed.relatedQuestionRecommendations?.status === 'READY' &&
        feed.relatedQuestionRecommendations.items.length > 0 && (
          // 추천 중 일부를 무작위로 골라 서버와 브라우저의 선택이 달라지므로 브라우저에서만 그립니다.
          <ClientOnly>
            <RelatedQuestions key={feed.feedId} items={feed.relatedQuestionRecommendations.items} />
          </ClientOnly>
        )}
      <div className="mt-10 mb-8 flex justify-center">
        <FeedLikeButton
          feedId={feed.feedId}
          likeCount={feed.likeCount}
          likedByMe={feed.likedByMe}
          label={isQuestion ? '저도 궁금해요' : '좋아요'}
          size="lg"
        />
      </div>
      <div className="flex flex-wrap items-center gap-2 border-b border-gray-100 pb-5">
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
        {/* 보는 사람(세션)에 따라 목록이 달라지므로 브라우저에서만 그립니다. */}
        <ClientOnly>
          <Comments
            feedId={feed.feedId}
            feedType={isQuestion ? 'QUESTION' : 'POST'}
            feedAuthorId={feed.author.userId ?? null}
          />
        </ClientOnly>
      </section>
    </article>
  );
}

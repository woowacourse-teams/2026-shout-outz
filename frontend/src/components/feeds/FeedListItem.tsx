import { Link } from '@tanstack/react-router';
import { IconHeart, IconHeartFilled, IconMessageCircle } from '@tabler/icons-react';
import type { Feed } from '@/apis/feed';
import { Image } from '@/components/Image';
import { FeedAuthor } from '@/components/feeds/FeedAuthor';
import { toPlainText } from '@/utils/markdown';
import { formatRelativeTime } from '@/utils/date';
import { analytics, type FeedSurface } from '@/utils/analytics';
import { cn } from '@/utils/cn';

/** 커뮤니티 목록의 한 줄. 카드 테두리 없이 위쪽 구분선으로 나눈다(구분선은 목록이 그린다). */
export function FeedListItem({ feed, surface }: { feed: Feed; surface: FeedSurface }) {
  const isQuestion = feed.feedType === 'QUESTION';
  const commentCount = feed.commentCount ?? 0;
  const likeCount = feed.likeCount ?? 0;
  const [firstMedia, ...restMedia] = [...feed.media].sort(
    (a, b) => a.displayOrder - b.displayOrder,
  );

  return (
    <article className="relative flex min-w-0 flex-col gap-2.5">
      <Link
        to="/community/$feedId"
        params={{ feedId: String(feed.feedId) }}
        onClick={() =>
          analytics.track({ name: 'feed_detail_opened', feedId: feed.feedId, from: surface })
        }
        aria-label={feed.title}
        className="focus-visible:outline-primary-600 absolute inset-0 z-10 rounded-sm focus-visible:outline-2 focus-visible:outline-offset-4"
      />
      <p className="flex flex-wrap items-center gap-2 text-xs text-gray-400">
        {isQuestion && (
          <span
            className={cn(
              'rounded-sm px-1.5 py-px font-bold',
              commentCount === 0
                ? 'bg-yellow-50 text-yellow-700'
                : 'bg-primary-50 text-primary-600',
            )}
          >
            {commentCount === 0 ? '답변 대기' : `답변 ${commentCount}`}
          </span>
        )}
        <span className="font-semibold text-gray-600">
          {feed.categories.map((category) => category.displayName).join(' · ') ||
            (isQuestion ? '질문' : '이야기')}
        </span>
        <time dateTime={feed.createdAt}>{formatRelativeTime(feed.createdAt)}</time>
      </p>
      <h3 className="text-lg font-bold tracking-tight break-words text-gray-900">
        {isQuestion && <span className="text-primary-600 mr-2">Q.</span>}
        {feed.title}
      </h3>
      <p className="line-clamp-2 text-sm leading-6 break-words text-gray-600">
        {toPlainText(feed.content)}
      </p>
      {firstMedia && (
        <div className="pointer-events-none relative w-36">
          <Image
            src={firstMedia.url}
            alt="피드 첨부 이미지"
            loading="lazy"
            className="h-24 w-36 rounded-lg bg-gray-50 object-contain"
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
      <div className="mt-1 flex items-center justify-between gap-3">
        <div className="min-w-0">
          <FeedAuthor
            author={feed.author}
            isAnonymous={feed.isAnonymous}
            avatarSize="sm"
            profileLink={false}
          />
        </div>
        <div
          className="flex shrink-0 items-center gap-3.5 text-xs text-gray-500"
          aria-label="반응 수"
        >
          <span
            className={cn('inline-flex items-center gap-1', feed.likedByMe && 'text-primary-600')}
            aria-label={`${isQuestion ? '궁금해요' : '좋아요'} ${likeCount}개${feed.likedByMe ? ', 내가 누름' : ''}`}
          >
            {feed.likedByMe ? (
              <IconHeartFilled className="size-4" aria-hidden="true" />
            ) : (
              <IconHeart className="size-4" aria-hidden="true" />
            )}
            {likeCount}
          </span>
          <span
            className="inline-flex items-center gap-1"
            aria-label={`${isQuestion ? '답변' : '댓글'} ${commentCount}개`}
          >
            <IconMessageCircle className="size-4" aria-hidden="true" />
            {commentCount}
          </span>
        </div>
      </div>
    </article>
  );
}

import { Link } from '@tanstack/react-router';
import { IconHeart, IconMessageCircle, IconSparkles } from '@tabler/icons-react';
import type { Feed } from '@/apis/feed';
import { Image } from '@/components/Image';
import { FeedAuthor } from '@/components/feeds/FeedAuthor';
import { toPlainText } from '@/utils/markdown';
import { formatRelativeTime } from '@/utils/date';
import { analytics, type FeedSurface } from '@/utils/analytics';

export function FeedCard({ feed, surface }: { feed: Feed; surface: FeedSurface }) {
  const [firstMedia, ...restMedia] = [...feed.media].sort(
    (a, b) => a.displayOrder - b.displayOrder,
  );

  return (
    <article className="group bg-background relative min-w-0 rounded-lg border border-gray-200 p-5 transition-colors hover:border-gray-300 md:p-6">
      <Link
        to="/community/$feedId"
        params={{ feedId: String(feed.feedId) }}
        onClick={() =>
          analytics.track({ name: 'feed_detail_opened', feedId: feed.feedId, from: surface })
        }
        aria-label={feed.title}
        className="focus-visible:outline-primary-600 absolute inset-0 z-10 rounded-lg focus-visible:outline-2 focus-visible:outline-offset-2"
      />
      <div className="mb-4 flex items-start justify-between gap-3">
        <div className="text-primary-600 flex flex-col items-start gap-3 text-xs leading-4 font-medium">
          {feed.feedType === 'QUESTION' && (feed.commentCount ?? 0) === 0 && (
            <span className="flex items-center gap-1">
              <IconSparkles className="size-3.5 text-yellow-500" aria-hidden="true" />
              답변을 기다리고 있어요
            </span>
          )}
          <span>
            {feed.categories.map((category) => category.displayName).join(' · ') ||
              (feed.feedType === 'QUESTION' ? '질문' : '이야기')}
          </span>
        </div>
        <time dateTime={feed.createdAt} className="shrink-0 text-xs leading-4 text-gray-500">
          {formatRelativeTime(feed.createdAt)}
        </time>
      </div>
      <div className="relative">
        <h3 className="group-hover:text-primary-600 text-base leading-6 font-semibold tracking-tight break-words text-gray-900">
          {feed.feedType === 'QUESTION' && <span className="text-primary-600 mr-1.5">Q.</span>}
          {feed.title}
        </h3>
        <p className="mt-2 line-clamp-2 text-sm leading-6 break-words text-gray-600">
          {toPlainText(feed.content)}
        </p>
        {firstMedia && (
          <div className="pointer-events-none relative mt-4 w-36">
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
      </div>
      <div className="mt-5 flex items-center justify-between gap-3 border-t border-gray-100 pt-4">
        <div className="min-w-0">
          <FeedAuthor
            author={feed.author}
            isAnonymous={feed.isAnonymous}
            avatarSize="sm"
            profileLink={false}
          />
        </div>
        <div
          className="flex shrink-0 items-center gap-3 text-xs text-gray-500"
          aria-label="반응 수"
        >
          <span
            className="inline-flex items-center gap-1"
            aria-label={`${feed.feedType === 'QUESTION' ? '궁금해요' : '좋아요'} ${feed.likeCount ?? 0}개`}
          >
            <IconHeart className="size-4" aria-hidden="true" />
            {feed.likeCount ?? 0}
          </span>
          <span
            className="inline-flex items-center gap-1"
            aria-label={`${feed.feedType === 'QUESTION' ? '답변' : '댓글'} ${feed.commentCount ?? 0}개`}
          >
            <IconMessageCircle className="size-4" aria-hidden="true" />
            {feed.commentCount ?? 0}
          </span>
        </div>
      </div>
    </article>
  );
}

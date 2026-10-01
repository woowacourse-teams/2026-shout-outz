import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { IconHeart, IconMessageCircle } from '@tabler/icons-react';
import { feedsQuery } from '@/apis/feed';
import type { FeedType } from '@/apis/feed';
import { FeedAuthor } from '@/components/feeds/FeedAuthor';
import { toPlainText } from '@/utils/markdown';
import { formatRelativeTime } from '@/utils/date';
import { analytics } from '@/utils/analytics';

export function PopularFeedList({ feedType }: { feedType?: FeedType }) {
  const { data } = useSuspenseInfiniteQuery(feedsQuery('POPULAR', undefined, 3, feedType));
  const feeds = data.pages[0]?.data ?? [];

  return (
    <aside aria-label={`인기 ${feedType === 'QUESTION' ? '질문' : '피드'}`}>
      <h2 className="mb-5 text-lg font-bold text-gray-900">
        인기 {feedType === 'QUESTION' ? '질문' : '피드'}
      </h2>
      <ul className="space-y-3">
        {feeds.map((feed) => {
          const excerpt = toPlainText(feed.content);
          return (
            <li
              key={feed.feedId}
              className="group bg-background relative min-w-0 rounded-lg border border-gray-200 p-4 transition-colors hover:border-gray-300"
            >
              <Link
                to="/community/$feedId"
                params={{ feedId: String(feed.feedId) }}
                aria-label={feed.title}
                className="focus-visible:outline-primary-600 absolute inset-0 z-10 rounded-lg focus-visible:outline-2 focus-visible:outline-offset-2"
                onClick={() =>
                  analytics.track({
                    name: 'feed_detail_opened',
                    feedId: feed.feedId,
                    from: 'feeds',
                  })
                }
              />
              <div className="mb-3 flex items-start justify-between gap-2">
                <span className="text-primary-600 min-w-0 text-xs leading-4 font-medium">
                  {feed.categories.map((category) => category.displayName).join(' · ') ||
                    (feed.feedType === 'QUESTION' ? '질문' : '이야기')}
                </span>
                <time
                  dateTime={feed.createdAt}
                  className="shrink-0 text-xs leading-4 text-gray-500"
                >
                  {formatRelativeTime(feed.createdAt)}
                </time>
              </div>
              <h3 className="group-hover:text-primary-600 line-clamp-2 text-sm leading-5 font-semibold tracking-tight break-words text-gray-900">
                {feed.feedType === 'QUESTION' && <span className="text-primary-600 mr-1">Q.</span>}
                {feed.title}
              </h3>
              {excerpt && (
                <p className="mt-2 line-clamp-2 text-xs leading-5 break-words text-gray-600">
                  {excerpt}
                </p>
              )}
              <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-gray-100 pt-3">
                <div className="min-w-0">
                  <FeedAuthor
                    author={feed.author}
                    isAnonymous={feed.isAnonymous}
                    avatarSize="xs"
                    profileLink={false}
                  />
                </div>
                <div
                  className="flex shrink-0 items-center gap-2 text-xs text-gray-500"
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
            </li>
          );
        })}
      </ul>
      {feeds.length === 0 && (
        <p className="rounded-lg border border-gray-200 px-4 py-8 text-center text-sm text-gray-500">
          {feedType === 'QUESTION' ? '아직 인기 질문이 없습니다.' : '아직 인기 피드가 없습니다.'}
        </p>
      )}
    </aside>
  );
}

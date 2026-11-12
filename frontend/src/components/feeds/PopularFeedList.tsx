import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import {
  IconChevronRight,
  IconHeart,
  IconHeartFilled,
  IconMessageCircle,
} from '@tabler/icons-react';
import { feedsQuery } from '@/apis/feed';
import type { Feed, FeedType } from '@/apis/feed';
import { Avatar } from '@/components/Avatar';
import { UserAffiliation } from '@/components/users/UserAffiliation';
import { formatRelativeTime } from '@/utils/date';
import { formatFeedCardLabel, formatFeedCategory } from '@/utils/feed';
import { analytics } from '@/utils/analytics';
import { cn } from '@/utils/cn';

const POPULAR_FEED_COUNT = 3;

/**
 * 카드를 덮는 상세 링크. 제목 안에 두고 가상 요소로 카드 전체를 덮어, 카드 어디를 눌러도 이동한다.
 * 스크린리더에서는 카드 하나가 제목 겸 링크 하나로 읽히도록, 카드 정보를 링크 이름에 모은다.
 */
function DetailLink({ feed }: { feed: Feed }) {
  return (
    <Link
      to="/community/$feedId"
      params={{ feedId: String(feed.feedId) }}
      aria-label={formatFeedCardLabel(feed)}
      className="focus-visible:after:outline-primary-600 outline-none after:absolute after:inset-0 after:z-10 after:rounded-sm focus-visible:after:outline-2 focus-visible:after:outline-offset-2"
      onClick={() =>
        analytics.track({ name: 'feed_detail_opened', feedId: feed.feedId, from: 'feeds' })
      }
    >
      {feed.feedType === 'QUESTION' && (
        <span aria-hidden="true" className="text-primary-600">
          Q.{' '}
        </span>
      )}
      {feed.title}
    </Link>
  );
}

/** 커뮤니티 목록 오른쪽의 인기 글. 질문 목록에서는 인기 피드를, 피드 목록에서는 인기 질문을 보여 준다. */
export function PopularFeedList({ feedType }: { feedType?: FeedType }) {
  const { data } = useSuspenseInfiniteQuery(
    feedsQuery('POPULAR', undefined, POPULAR_FEED_COUNT, feedType),
  );
  const feeds = data.pages[0]?.data ?? [];
  const isQuestion = feedType === 'QUESTION';
  const label = isQuestion ? '인기 질문' : '인기글';

  return (
    <aside aria-label={label} className="border-l border-gray-200 pl-6">
      <div className="flex items-center justify-between gap-2 pb-3">
        <h2 className="text-sm font-bold tracking-tight text-gray-900">{label}</h2>
        {/* "모두 보기"는 피드 목록을 인기순으로 연다. */}
        {!isQuestion && (
          <Link
            to="/community"
            search={{ type: 'POST', sort: 'POPULAR' }}
            className="focus-visible:outline-primary-600 inline-flex shrink-0 items-center gap-0.5 rounded-sm text-xs font-semibold text-gray-500 hover:text-gray-700 focus-visible:outline-2"
          >
            모두 보기
            <IconChevronRight className="size-3.5" aria-hidden="true" />
          </Link>
        )}
      </div>
      <ul>
        {feeds.map((feed) => {
          const isQuestionFeed = feed.feedType === 'QUESTION';
          const likeCount = feed.likeCount ?? 0;
          const commentCount = feed.commentCount ?? 0;
          return (
            <li
              key={feed.feedId}
              className="relative flex flex-col gap-1.5 border-t border-gray-100 py-4"
            >
              <h3 className="order-2 line-clamp-3 text-sm leading-snug font-bold tracking-tight break-keep text-gray-900">
                <DetailLink feed={feed} />
              </h3>
              {/* 아래 정보는 링크 이름으로 함께 읽으므로, 같은 내용을 다시 읽지 않게 숨긴다. */}
              <p aria-hidden="true" className="order-1 text-xs font-medium text-gray-500">
                <span className="text-primary-600">{isQuestionFeed ? '질문' : '피드'}</span> ·{' '}
                {formatFeedCategory(feed)} ·{' '}
                <time dateTime={feed.createdAt}>{formatRelativeTime(feed.createdAt)}</time>
              </p>
              <div
                aria-hidden="true"
                className="order-3 flex min-w-0 items-center gap-2 text-xs text-gray-500"
              >
                <span className="flex min-w-0 flex-1 items-center gap-1.5">
                  <Avatar
                    size="xs"
                    anonymous={!feed.author.handle}
                    src={feed.author.handle ? feed.author.avatarUrl : undefined}
                    name={feed.author.displayName ?? undefined}
                    alt=""
                  />
                  <span className="min-w-0 truncate font-semibold text-gray-600">
                    {feed.author.handle ? feed.author.displayName : '익명'}
                  </span>
                  <UserAffiliation
                    {...feed.author}
                    anonymous={!feed.author.handle}
                    className="min-w-0 flex-1 truncate leading-normal whitespace-nowrap"
                  />
                </span>
                <span className="flex shrink-0 gap-2.5">
                  <span
                    className={cn(
                      'inline-flex items-center gap-1',
                      feed.likedByMe && 'text-primary-600',
                    )}
                  >
                    {feed.likedByMe ? (
                      <IconHeartFilled className="size-3.5" aria-hidden="true" />
                    ) : (
                      <IconHeart className="size-3.5" aria-hidden="true" />
                    )}
                    {likeCount}
                  </span>
                  <span className="inline-flex items-center gap-1">
                    <IconMessageCircle className="size-3.5" aria-hidden="true" />
                    {commentCount}
                  </span>
                </span>
              </div>
            </li>
          );
        })}
      </ul>
      {feeds.length === 0 && (
        <p className="border-t border-gray-100 py-8 text-center text-sm text-gray-500">
          {isQuestion ? '아직 인기 질문이 없습니다.' : '아직 인기 피드가 없습니다.'}
        </p>
      )}
    </aside>
  );
}

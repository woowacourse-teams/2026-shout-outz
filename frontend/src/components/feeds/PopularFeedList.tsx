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
import { formatRelativeTime } from '@/utils/date';
import { analytics } from '@/utils/analytics';
import { cn } from '@/utils/cn';

const POPULAR_FEED_COUNT = 3;

function categoryOf(feed: Feed) {
  return (
    feed.categories.map((category) => category.displayName).join(' · ') ||
    (feed.feedType === 'QUESTION' ? '질문' : '이야기')
  );
}

function DetailLink({ feed }: { feed: Feed }) {
  return (
    <Link
      to="/community/$feedId"
      params={{ feedId: String(feed.feedId) }}
      aria-label={feed.title}
      className="focus-visible:outline-primary-600 absolute inset-0 z-10 rounded-sm focus-visible:outline-2 focus-visible:outline-offset-2"
      onClick={() =>
        analytics.track({ name: 'feed_detail_opened', feedId: feed.feedId, from: 'feeds' })
      }
    />
  );
}

/** 커뮤니티 목록 오른쪽의 인기 글. 질문 목록에서는 인기 피드를, 피드 목록에서는 인기 질문을 보여 준다. */
export function PopularFeedList({ feedType }: { feedType?: FeedType }) {
  const { data } = useSuspenseInfiniteQuery(
    feedsQuery('POPULAR', undefined, POPULAR_FEED_COUNT, feedType),
  );
  const feeds = data.pages[0]?.data ?? [];
  const isQuestion = feedType === 'QUESTION';
  const label = `인기 ${isQuestion ? '질문' : '피드'}`;

  return (
    // DESIGN-ISSUE: 프로토타입은 질문 화면의 인기 피드(왼쪽 구분선, 설명, 모두 보기, 작성자·반응 글자)와
    // 피드 화면의 인기 질문(구분선·설명 없음, 날짜, 아이콘 반응 수) 모양이 달라 각각 따랐다. 한 모양으로 맞추길 제안한다.
    <aside aria-label={label} className={cn(!isQuestion && 'border-l border-gray-200 pl-6')}>
      {isQuestion ? (
        <h2 className="pb-3 text-base font-bold text-gray-900">{label}</h2>
      ) : (
        <div className="flex items-start justify-between gap-3 pb-3">
          <div className="flex flex-col gap-1">
            <h2 className="text-sm font-bold text-gray-900">
              {label} <span className="font-medium text-gray-400">{feeds.length}</span>
            </h2>
            <p className="text-xs text-gray-500">크루들이 요즘 많이 본 이야기예요.</p>
          </div>
          {/* ASSUMPTION: "모두 보기"는 피드 목록을 인기순으로 연다. */}
          <Link
            to="/community"
            search={{ type: 'POST', sort: 'POPULAR' }}
            className="focus-visible:outline-primary-600 inline-flex shrink-0 items-center gap-0.5 rounded-sm text-xs font-semibold text-gray-500 hover:text-gray-700 focus-visible:outline-2"
          >
            모두 보기
            <IconChevronRight className="size-3.5" aria-hidden="true" />
          </Link>
        </div>
      )}
      <ul>
        {feeds.map((feed) =>
          isQuestion ? (
            <li key={feed.feedId} className="relative border-t border-gray-100 py-3.5">
              <DetailLink feed={feed} />
              <p className="flex items-center gap-2 text-xs text-gray-400">
                <span className="font-semibold text-gray-600">{categoryOf(feed)}</span>
                <time dateTime={feed.createdAt}>{formatRelativeTime(feed.createdAt)}</time>
              </p>
              <h3 className="mt-1.5 line-clamp-2 text-sm font-bold break-words text-gray-900">
                <span className="text-primary-600 mr-1.5">Q.</span>
                {feed.title}
              </h3>
              <div className="mt-1.5 flex items-center gap-3 text-xs text-gray-500">
                <span
                  className={cn(
                    'inline-flex items-center gap-1',
                    feed.likedByMe && 'text-primary-600',
                  )}
                  aria-label={`궁금해요 ${feed.likeCount ?? 0}개${feed.likedByMe ? ', 내가 누름' : ''}`}
                >
                  {feed.likedByMe ? (
                    <IconHeartFilled className="size-4" aria-hidden="true" />
                  ) : (
                    <IconHeart className="size-4" aria-hidden="true" />
                  )}
                  {feed.likeCount ?? 0}
                </span>
                <span
                  className="inline-flex items-center gap-1"
                  aria-label={`답변 ${feed.commentCount ?? 0}개`}
                >
                  <IconMessageCircle className="size-4" aria-hidden="true" />
                  {feed.commentCount ?? 0}
                </span>
              </div>
            </li>
          ) : (
            <li
              key={feed.feedId}
              className="relative flex flex-col gap-1.5 border-t border-gray-100 py-4"
            >
              <DetailLink feed={feed} />
              <p className="text-xs font-medium text-gray-500">
                <span className="text-primary-600">피드</span> · {categoryOf(feed)}
              </p>
              <h3 className="line-clamp-3 text-sm font-bold break-words text-gray-900">
                {feed.title}
              </h3>
              <p className="text-xs text-gray-400">
                {feed.author.handle ? feed.author.displayName : '익명'} · 좋아요{' '}
                {feed.likeCount ?? 0} · 댓글 {feed.commentCount ?? 0}
              </p>
            </li>
          ),
        )}
      </ul>
      {feeds.length === 0 && (
        <p className="border-t border-gray-100 py-8 text-center text-sm text-gray-500">
          {isQuestion ? '아직 인기 질문이 없습니다.' : '아직 인기 피드가 없습니다.'}
        </p>
      )}
    </aside>
  );
}

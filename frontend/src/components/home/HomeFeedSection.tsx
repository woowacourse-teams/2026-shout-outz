import { startTransition, useState } from 'react';
import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { IconMessage, IconMessageQuestion } from '@tabler/icons-react';

import { feedsQuery } from '@/apis/feed';
import { FeedCard } from '@/components/feeds/FeedCard';
import { getButtonStyles } from '@/components/Button';
import { Tab } from '@/components/Tab';
import { type FeedSort, type FeedType } from '@/types/feed';
import { analytics } from '@/utils/analytics';

const HOME_FEED_SIZE = 3;

const SORT_TABS: { value: FeedSort; label: string }[] = [
  { value: 'LATEST', label: '최신순' },
  { value: 'POPULAR', label: '인기순' },
];

export function HomeFeedSection() {
  // TODO 현재는 홈에서 보는 정보는 미리보기 용이기 때문에 url로 관리하지 않기로 결정, 추후에 논의 필요
  const [sort, setSort] = useState<FeedSort>('LATEST');
  const [feedType, setFeedType] = useState<FeedType>('QUESTION');
  const { data } = useSuspenseInfiniteQuery(feedsQuery(sort, undefined, HOME_FEED_SIZE, feedType));
  const feeds = data.pages[0]?.data ?? [];
  const label = feedType === 'QUESTION' ? '질문' : '피드';

  // TODO fallback을 보여주지 않지만 반응이 없는 것처럼 보일 수도 있음. 논의 후에 적용하면 좋을 것 같아서 남겨둠
  const changeSort = (value: string) => {
    analytics.track({ name: 'feed_sort_changed', sort: value, surface: 'home' });
    startTransition(() => setSort(value as FeedSort));
  };
  const changeType = (value: string) => {
    startTransition(() => {
      setFeedType(value as FeedType);
      setSort('LATEST');
    });
  };

  return (
    <section aria-label="커뮤니티" className="flex flex-col gap-3">
      <div className="flex items-center justify-between gap-2 pb-1">
        <h2 className="text-base font-bold tracking-tight text-gray-900">커뮤니티</h2>
      </div>
      <Tab
        variant="subnav"
        size="sm"
        className="w-full border-b border-gray-200"
        value={feedType}
        onChange={changeType}
        aria-label="커뮤니티 유형"
      >
        <Tab.Item value="QUESTION">
          <IconMessageQuestion className="mr-1.5 size-4" aria-hidden="true" />
          질문
        </Tab.Item>
        <Tab.Item value="POST">
          <IconMessage className="mr-1.5 size-4" aria-hidden="true" />
          피드
        </Tab.Item>
      </Tab>
      <Tab variant="chip" size="sm" value={sort} onChange={changeSort} aria-label={`${label} 정렬`}>
        {SORT_TABS.map(({ value, label: sortLabel }) => (
          <Tab.Item key={value} value={value}>
            {sortLabel}
          </Tab.Item>
        ))}
      </Tab>

      {feeds.length === 0 ? (
        <p className="py-8 text-center text-sm text-gray-500">
          {feedType === 'QUESTION'
            ? '아직 작성된 질문이 없습니다.'
            : '아직 작성된 피드가 없습니다.'}
        </p>
      ) : (
        <ul className="space-y-3">
          {feeds.map((feed) => (
            <li key={feed.feedId} className="min-w-0">
              <FeedCard feed={feed} surface="home" />
            </li>
          ))}
        </ul>
      )}
      <Link
        to="/community"
        search={{ type: feedType, sort }}
        className={getButtonStyles({ variant: 'outline', className: 'w-full' })}
        onClick={() => analytics.track({ name: 'section_more_clicked', target: 'feeds' })}
      >
        {label} 전체보기 ›
      </Link>
    </section>
  );
}

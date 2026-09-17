import { startTransition, useState } from 'react';
import { useSuspenseQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';

import { feedListQueryOptions } from '@/api/feed';
import { FeedCard } from '@/components/feeds/FeedCard';
import { Tab } from '@/components/Tab';
import { type FeedSort } from '@/types/feed';

const HOME_FEED_SIZE = 3;

const SORT_TABS: { value: FeedSort; label: string }[] = [
  { value: 'LATEST', label: '최신순' },
  { value: 'POPULAR', label: '인기순' },
];

export function HomeFeedSection() {
  // TODO 현재는 홈에서 보는 정보는 미리보기 용이기 때문에 url로 관리하지 않기로 결정, 추후에 논의 필요
  const [sort, setSort] = useState<FeedSort>('LATEST');
  const { data: feeds } = useSuspenseQuery(feedListQueryOptions({ sort, size: HOME_FEED_SIZE }));

  // TODO fallback을 보여주지 않지만 반응이 없는 것처럼 보일 수도 있음. 논의 후에 적용하면 좋을 것 같아서 남겨둠
  const changeSort = (value: string) => startTransition(() => setSort(value as FeedSort));

  return (
    <section aria-label="피드" className="flex flex-col gap-3">
      <div className="flex items-center justify-between gap-2 pb-1">
        <Tab variant="chip" size="sm" value={sort} onChange={changeSort} aria-label="피드 정렬">
          {SORT_TABS.map(({ value, label }) => (
            <Tab.Item key={value} value={value}>
              {label}
            </Tab.Item>
          ))}
        </Tab>

        <Link
          to="/feeds"
          className="text-primary-600 focus-visible:outline-primary-600 shrink-0 rounded-sm text-sm font-bold focus-visible:outline-2"
        >
          피드 전체보기 ›
        </Link>
      </div>

      {feeds.length === 0 ? (
        <p className="py-8 text-center text-sm text-gray-500">아직 작성된 피드가 없습니다.</p>
      ) : (
        <ul>
          {feeds.map((feed) => (
            <li key={feed.feedId} className="min-w-0">
              <FeedCard feed={feed} />
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

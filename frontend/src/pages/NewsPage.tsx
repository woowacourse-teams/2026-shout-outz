import { getRouteApi } from '@tanstack/react-router';

import { getNewsList } from '@/mocks/news';
import { NewsItem } from '@/components/NewsItem';
import { Select } from '@/components/Select';
import { Tab } from '@/components/Tab';
import {
  DEFAULT_NEWS_FILTER,
  DEFAULT_NEWS_SORT,
  NEWS_FILTERS,
  NEWS_SORTS,
  type NewsFilter,
  type NewsSort,
} from '@/types/news';

const FILTER_LABELS: Record<NewsFilter, string> = {
  ALL: '전체',
  NOTICE: '공지사항',
  EVENT: '이벤트',
};

const SORT_LABELS: Record<NewsSort, string> = {
  LATEST: '최신순',
};

const route = getRouteApi('/news/');

export function NewsPage() {
  const { type, sort } = route.useSearch();
  const navigate = route.useNavigate();

  const filter = type ?? DEFAULT_NEWS_FILTER;

  const setSearch = (next: { type?: NewsFilter; sort?: NewsSort }) => {
    navigate({ search: (previous) => ({ ...previous, ...next }) });
  };

  // TODO api 연동 시에 쿼리로 변경
  const allNews = getNewsList();
  const news = filter === 'ALL' ? allNews : allNews.filter((item) => item.type === filter);

  return (
    <main className="flex flex-col gap-5 px-4 pt-5 pb-7 md:gap-7 md:px-16 md:pt-10 md:pb-20">
      <header className="flex flex-col gap-1 md:gap-2">
        <h1 className="text-lg font-bold tracking-tight text-gray-900 md:text-2xl">소식</h1>
        <p className="text-sm text-gray-600">우아한테크코스 공식 공지사항 및 크루 참여 이벤트</p>
      </header>

      <div className="flex items-center justify-between gap-2">
        <Tab
          variant="chip"
          size="sm"
          value={filter}
          onChange={(value) => setSearch({ type: value as NewsFilter })}
          aria-label="소식 분류"
        >
          {NEWS_FILTERS.map((value) => (
            <Tab.Item key={value} value={value}>
              {FILTER_LABELS[value]}
            </Tab.Item>
          ))}
        </Tab>

        <div className="w-24 shrink-0">
          <Select
            value={sort ?? DEFAULT_NEWS_SORT}
            onValueChange={(value) => setSearch({ sort: value as NewsSort })}
            aria-label="소식 정렬"
            className="h-auto px-3 py-1.5 text-xs"
          >
            {NEWS_SORTS.map((value) => (
              <Select.Item key={value} value={value}>
                {SORT_LABELS[value]}
              </Select.Item>
            ))}
          </Select>
        </div>
      </div>

      <ul className="flex flex-col gap-5 md:grid md:grid-cols-2 md:gap-x-10 md:gap-y-7">
        {news.map(({ id, ...item }) => (
          <li
            key={id}
            className="border-b border-gray-100 pb-4 last:border-b-0 last:pb-0 md:pb-6 md:nth-last-[-n+2]:border-b-0"
          >
            <NewsItem {...item} />
          </li>
        ))}
      </ul>
    </main>
  );
}

import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { getRouteApi, Link } from '@tanstack/react-router';
import { newsInfiniteQueryOptions } from '@/api/news';
import { AppGnb } from '@/components/AppGnb';
import { Footer } from '@/components/Footer';
import { NewsItem } from '@/components/NewsItem';
import { Select } from '@/components/Select';
import { Tab } from '@/components/Tab';
import { NewsListBoundary } from '@/components/news/NewsListBoundary';
import { DEFAULT_NEWS_FILTER, DEFAULT_NEWS_SORT, NEWS_FILTERS, NEWS_SORTS } from '@/constants/news';
import type { NewsFilter, NewsSort } from '@/types/news';

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
  const sortBy = sort ?? DEFAULT_NEWS_SORT;

  const setSearch = (next: { type?: NewsFilter; sort?: NewsSort }) => {
    navigate({ search: (previous) => ({ ...previous, ...next }) });
  };

  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <title>소식 | shout-outz</title>
      <AppGnb aria-label="주요 헤더" />
      <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-5 px-4 pt-6 pb-12 md:gap-7 md:pt-10 md:pb-20">
        <div className="flex flex-col gap-1 md:gap-2">
          <h1 className="text-xl font-bold tracking-tight text-gray-900 md:text-2xl">소식</h1>
          <p className="text-sm text-gray-600">우아한테크코스 공식 공지사항 및 크루 참여 이벤트</p>
        </div>

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
              value={sortBy}
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

        <NewsListBoundary key={`${filter}-${sortBy}`}>
          <NewsList filter={filter} sort={sortBy} />
        </NewsListBoundary>
      </main>
      <Footer />
    </div>
  );
}

function NewsList({ filter, sort }: { filter: NewsFilter; sort: NewsSort }) {
  const query = useSuspenseInfiniteQuery(newsInfiniteQueryOptions(filter, sort));
  const news = query.data.pages.flatMap((page) => page.data);

  if (news.length === 0) {
    return <p className="py-16 text-center text-sm text-gray-500">등록된 소식이 없습니다.</p>;
  }

  return (
    <>
      <ul className="flex flex-col gap-5 md:grid md:grid-cols-2 md:gap-x-10 md:gap-y-7">
        {news.map(({ id, ...item }) => (
          <li
            key={id}
            className="border-b border-gray-100 pb-4 last:border-b-0 last:pb-0 md:pb-6 md:nth-last-[-n+2]:border-b-0"
          >
            <Link
              to="/news/$newsId"
              params={{ newsId: String(id) }}
              className="focus-visible:outline-primary-600 block rounded-sm focus-visible:outline-2"
            >
              <NewsItem {...item} />
            </Link>
          </li>
        ))}
      </ul>
      {query.hasNextPage && (
        <button
          type="button"
          className="border-primary-600 text-primary-600 mt-2 rounded-lg border px-4 py-2 text-sm font-semibold"
          disabled={query.isFetchingNextPage}
          onClick={() => void query.fetchNextPage({ cancelRefetch: false })}
        >
          {query.isFetchingNextPage ? '불러오는 중…' : '소식 더 보기'}
        </button>
      )}
    </>
  );
}

import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { getRouteApi, Link } from '@tanstack/react-router';
import { newsInfiniteQueryOptions } from '@/api/news';
import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Button } from '@/components/Button';
import { Footer } from '@/components/Footer';
import { NewsItem } from '@/components/NewsItem';
import { Tab } from '@/components/Tab';
import { DEFAULT_NEWS_FILTER, DEFAULT_NEWS_SORT, NEWS_FILTERS } from '@/constants/news';
import type { NewsFilter, NewsSort } from '@/types/news';
import { analytics } from '@/utils/analytics';

const FILTER_LABELS: Record<NewsFilter, string> = {
  ALL: '전체',
  NOTICE: '공지사항',
  EVENT: '이벤트',
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
      <main className="mx-auto flex w-full max-w-4xl flex-1 flex-col gap-5 px-5 pt-6 pb-12 md:px-12 md:pt-10 md:pb-20">
        <div className="flex flex-col gap-1">
          <h1 className="text-xl font-bold tracking-tight text-gray-900 md:text-2xl">소식</h1>
          <p className="text-sm text-gray-600">공지사항 및 이벤트</p>
        </div>

        <div className="border-b border-gray-200 pb-3">
          <Tab
            variant="chip"
            size="sm"
            className="flex-wrap"
            value={filter}
            onChange={(value) => {
              analytics.track({ name: 'news_filter_changed', type: value });
              setSearch({ type: value as NewsFilter });
            }}
            aria-label="소식 분류"
          >
            {NEWS_FILTERS.map((value) => (
              <Tab.Item key={value} value={value}>
                {FILTER_LABELS[value]}
              </Tab.Item>
            ))}
          </Tab>
        </div>

        <AsyncBoundary
          key={`${filter}-${sortBy}`}
          fallback={
            <p role="status" className="py-16 text-center text-sm text-gray-500">
              소식을 불러오는 중…
            </p>
          }
          errorFallback={(_error, reset) => (
            <div role="alert" className="space-y-4 py-16 text-center">
              <p className="text-gray-600">소식을 불러오지 못했습니다.</p>
              <Button variant="outline" onClick={reset}>
                다시 시도
              </Button>
            </div>
          )}
        >
          <NewsList filter={filter} sort={sortBy} />
        </AsyncBoundary>
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
      <ul className="flex flex-col gap-3">
        {news.map(({ id, type, title, summary, publishedAt }) => (
          <li key={id} className="group min-w-0">
            <Link
              to="/news/$newsId"
              params={{ newsId: String(id) }}
              onClick={() => {
                analytics.track({ name: 'card_clicked', target: 'news', surface: 'news' });
                analytics.track({ name: 'news_detail_opened', newsId: id, type, from: 'news' });
              }}
              className="bg-background focus-visible:outline-primary-600 block rounded-lg border border-gray-200 p-5 transition-colors hover:border-gray-300 focus-visible:outline-2 focus-visible:outline-offset-2 md:p-6"
            >
              <NewsItem type={type} title={title} summary={summary} publishedAt={publishedAt} />
            </Link>
          </li>
        ))}
      </ul>
      {query.hasNextPage && (
        <Button
          variant="outline"
          className="mt-2 self-center"
          disabled={query.isFetchingNextPage}
          onClick={() => void query.fetchNextPage({ cancelRefetch: false })}
        >
          {query.isFetchingNextPage ? '불러오는 중…' : '소식 더 보기'}
        </Button>
      )}
    </>
  );
}

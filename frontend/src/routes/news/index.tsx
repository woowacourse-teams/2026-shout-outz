import { Suspense } from 'react';
import { createFileRoute } from '@tanstack/react-router';
import { NewsPage } from '@/pages/NewsPage';
import { isNewsFilter, isNewsSort, type NewsFilter, type NewsSort } from '@/types/news';

interface NewsSearch {
  type?: NewsFilter;
  sort?: NewsSort;
}

export const Route = createFileRoute('/news/')({
  validateSearch: (search: Record<string, unknown>): NewsSearch => ({
    type: isNewsFilter(search.type) ? search.type : undefined,
    sort: isNewsSort(search.sort) ? search.sort : undefined,
  }),
  staticData: {
    prerender: true,
  },
  component: RouteComponent,
  errorComponent: NewsListError,
});

function RouteComponent() {
  return (
    <Suspense fallback={<NewsListMessage>소식을 불러오는 중…</NewsListMessage>}>
      <NewsPage />
    </Suspense>
  );
}

function NewsListError() {
  return <NewsListMessage>소식을 불러오지 못했습니다.</NewsListMessage>;
}

function NewsListMessage({ children }: { children: string }) {
  return (
    <main className="px-4 pt-5 pb-7 md:px-16 md:pt-10 md:pb-20">
      <p className="text-sm text-gray-600">{children}</p>
    </main>
  );
}

import { Suspense } from 'react';
import { createFileRoute } from '@tanstack/react-router';

import { fetchNewsList } from '@/api/news';
import { DEFAULT_NEWS_FILTER, DEFAULT_NEWS_SORT } from '@/types/news';
import { NewsDetailPage } from '@/pages/NewsDetailPage';

export const Route = createFileRoute('/news/$newsId')({
  staticData: {
    prerender: true,
    generateStaticParams: async () => {
      const news = await fetchNewsList(DEFAULT_NEWS_FILTER, DEFAULT_NEWS_SORT);
      return news.map(({ id }) => ({ newsId: String(id) }));
    },
  },
  component: RouteComponent,
  errorComponent: NewsDetailError,
});

function RouteComponent() {
  return (
    <Suspense fallback={<NewsDetailMessage>소식을 불러오는 중…</NewsDetailMessage>}>
      <NewsDetailPage />
    </Suspense>
  );
}

function NewsDetailError() {
  return <NewsDetailMessage>소식을 불러오지 못했습니다.</NewsDetailMessage>;
}

function NewsDetailMessage({ children }: { children: string }) {
  return (
    <main className="px-4 pt-5 pb-7 md:px-16 md:pt-10 md:pb-20">
      <p className="text-sm text-gray-600">{children}</p>
    </main>
  );
}

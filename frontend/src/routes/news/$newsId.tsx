import { createFileRoute } from '@tanstack/react-router';

import { fetchNewsList } from '@/api/news';
import { DEFAULT_NEWS_FILTER, DEFAULT_NEWS_SORT } from '@/constants/news';
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
});

function RouteComponent() {
  return <NewsDetailPage />;
}

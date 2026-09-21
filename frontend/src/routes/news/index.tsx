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
  component: RouteComponent,
});

function RouteComponent() {
  return <NewsPage />;
}

import { createFileRoute } from '@tanstack/react-router';
import { NewsPage } from '@/pages/NewsPage';
import { isNewsFilter, isNewsSort, type NewsFilter, type NewsSort } from '@/types/news';

interface NewsSearch {
  type?: NewsFilter;
  sort?: NewsSort;
}

export const Route = createFileRoute('/news/')({
  // 미리 만들지 않는 목록 화면이지만 검색에 노출하도록 sitemap.xml에 넣는다.
  staticData: { sitemap: true },
  validateSearch: (search: Record<string, unknown>): NewsSearch => ({
    type: isNewsFilter(search.type) ? search.type : undefined,
    sort: isNewsSort(search.sort) ? search.sort : undefined,
  }),
  component: RouteComponent,
});

function RouteComponent() {
  return <NewsPage />;
}

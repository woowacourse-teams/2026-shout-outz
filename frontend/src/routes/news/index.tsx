import { createFileRoute } from '@tanstack/react-router';

import {
  isNewsFilter,
  isNewsSort,
  NewsPage,
  type NewsFilter,
  type NewsSort,
} from '@/pages/NewsPage';

// `<Link to="/news">`마다 search를 넘기는 것을 강제하는 걸 방지하기 위해 optional로
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
  component: NewsPage,
});

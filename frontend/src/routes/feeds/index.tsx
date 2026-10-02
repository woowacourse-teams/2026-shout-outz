import { createFileRoute, Navigate } from '@tanstack/react-router';
import type { FeedSort, FeedType } from '@/apis/feed';

export const Route = createFileRoute('/feeds/')({
  validateSearch: (search: Record<string, unknown>): { sort: FeedSort; type: FeedType } => {
    const type = search.type === 'QUESTION' ? 'QUESTION' : 'POST';
    return {
      type,
      sort:
        search.sort === 'POPULAR' || (type === 'QUESTION' && search.sort === 'WAITING')
          ? search.sort
          : 'LATEST',
    };
  },
  component: LegacyFeedList,
});

function LegacyFeedList() {
  const search = Route.useSearch();
  return <Navigate to="/community" search={search} replace />;
}

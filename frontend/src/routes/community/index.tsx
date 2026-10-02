import { createFileRoute } from '@tanstack/react-router';
import { FeedsPage } from '@/pages/FeedsPage';
import type { FeedSort, FeedType } from '@/apis/feed';
export const Route = createFileRoute('/community/')({
  validateSearch: (search: Record<string, unknown>): { sort?: FeedSort; type?: FeedType } => {
    const type = search.type === 'POST' ? 'POST' : 'QUESTION';
    return {
      type,
      sort:
        search.sort === 'POPULAR' || (type === 'QUESTION' && search.sort === 'WAITING')
          ? search.sort
          : 'LATEST',
    };
  },
  component: FeedRoute,
});
function FeedRoute() {
  const { sort, type } = Route.useSearch();
  const navigate = Route.useNavigate();
  return (
    <FeedsPage
      feedType={type ?? 'QUESTION'}
      onTypeChange={(value) => void navigate({ search: { type: value, sort: 'LATEST' } })}
      onCreate={() => void navigate({ to: '/community/new', search: { type: type ?? 'QUESTION' } })}
      sort={sort ?? 'LATEST'}
      onSortChange={(value) => void navigate({ search: { type, sort: value } })}
    />
  );
}

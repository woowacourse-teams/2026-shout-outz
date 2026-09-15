import { createFileRoute } from '@tanstack/react-router';
import { FeedsPage } from '@/pages/FeedsPage';
import type { FeedSort } from '@/apis/feed';
export const Route = createFileRoute('/feeds/')({
  validateSearch: (search: Record<string, unknown>): { sort?: FeedSort } => ({
    sort: search.sort === 'POPULAR' ? 'POPULAR' : 'LATEST',
  }),
  component: FeedRoute,
});
function FeedRoute() {
  const { sort } = Route.useSearch();
  const navigate = Route.useNavigate();
  return (
    <FeedsPage
      sort={sort ?? 'LATEST'}
      onSortChange={(value) => void navigate({ search: { sort: value } })}
    />
  );
}

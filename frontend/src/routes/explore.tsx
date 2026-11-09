import { createFileRoute } from '@tanstack/react-router';

import { ExplorePage } from '@/pages/ExplorePage';

interface ExploreSearch {
  q?: string;
}

// API가 생길 때까지 CSR로만 둔다(prerender·ssr을 지정하지 않는다).
export const Route = createFileRoute('/explore')({
  validateSearch: (search: Record<string, unknown>): ExploreSearch => {
    const q = typeof search.q === 'string' ? search.q.trim() : '';
    return q ? { q } : {};
  },
  component: ExploreRoute,
});

function ExploreRoute() {
  const { q } = Route.useSearch();
  const navigate = Route.useNavigate();

  return (
    <ExplorePage query={q ?? ''} onSearch={(query) => void navigate({ search: { q: query } })} />
  );
}

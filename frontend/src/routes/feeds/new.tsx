import { createFileRoute, Navigate } from '@tanstack/react-router';
import type { FeedType } from '@/apis/feed';

export const Route = createFileRoute('/feeds/new')({
  validateSearch: (search: Record<string, unknown>): { type: FeedType } => ({
    type: search.type === 'QUESTION' ? 'QUESTION' : 'POST',
  }),
  component: LegacyFeedCreate,
});

function LegacyFeedCreate() {
  return <Navigate to="/community/new" search={Route.useSearch()} replace />;
}

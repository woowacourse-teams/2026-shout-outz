import { createFileRoute } from '@tanstack/react-router';
import { FeedEditorPage } from '@/pages/FeedEditorPage';
import type { FeedType } from '@/apis/feed';

export const Route = createFileRoute('/community/new')({
  validateSearch: (search: Record<string, unknown>): { type: FeedType } => ({
    type: search.type === 'QUESTION' ? 'QUESTION' : 'POST',
  }),
  component: FeedCreateRoute,
});

function FeedCreateRoute() {
  const { type } = Route.useSearch();
  const navigate = Route.useNavigate();
  return (
    <FeedEditorPage
      feedType={type}
      onCancel={() => void navigate({ to: '/community', search: { type, sort: 'LATEST' } })}
      onSaved={(feedId) =>
        void navigate({ to: '/community/$feedId', params: { feedId: String(feedId) } })
      }
    />
  );
}

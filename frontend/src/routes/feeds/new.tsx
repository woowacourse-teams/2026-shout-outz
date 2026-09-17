import { createFileRoute } from '@tanstack/react-router';
import { FeedEditorPage } from '@/pages/FeedEditorPage';

export const Route = createFileRoute('/feeds/new')({ component: FeedCreateRoute });

function FeedCreateRoute() {
  const navigate = Route.useNavigate();
  return (
    <FeedEditorPage
      onCancel={() => void navigate({ to: '/feeds', search: { sort: 'LATEST' } })}
      onSaved={(feedId) =>
        void navigate({ to: '/feeds/$feedId', params: { feedId: String(feedId) } })
      }
    />
  );
}

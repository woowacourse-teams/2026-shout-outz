import { createFileRoute } from '@tanstack/react-router';
import { FeedEditorPage } from '@/pages/FeedEditorPage';

export const Route = createFileRoute('/feeds/$feedId_/edit')({ component: FeedEditRoute });

function FeedEditRoute() {
  const { feedId } = Route.useParams();
  const navigate = Route.useNavigate();
  const goToDetail = () => void navigate({ to: '/feeds/$feedId', params: { feedId } });
  return <FeedEditorPage feedId={Number(feedId)} onCancel={goToDetail} onSaved={goToDetail} />;
}

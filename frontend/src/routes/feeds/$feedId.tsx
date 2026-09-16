import { createFileRoute } from '@tanstack/react-router';
import { FeedDetailPage } from '@/pages/FeedDetailPage';

export const Route = createFileRoute('/feeds/$feedId')({
  component: FeedDetailRoute,
});

function FeedDetailRoute() {
  const { feedId } = Route.useParams();
  return <FeedDetailPage feedId={Number(feedId)} />;
}

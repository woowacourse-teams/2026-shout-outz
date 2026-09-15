import { createFileRoute } from '@tanstack/react-router';
import { FeedDetailPage } from '@/pages/FeedDetailPage';

export const Route = createFileRoute('/feeds/$postId')({
  component: FeedDetailRoute,
});

function FeedDetailRoute() {
  const { postId } = Route.useParams();
  return <FeedDetailPage postId={Number(postId)} />;
}

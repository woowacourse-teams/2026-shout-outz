import { createFileRoute, Navigate } from '@tanstack/react-router';

export const Route = createFileRoute('/feeds/$feedId')({ component: LegacyFeedDetail });

function LegacyFeedDetail() {
  return <Navigate to="/community/$feedId" params={Route.useParams()} replace />;
}

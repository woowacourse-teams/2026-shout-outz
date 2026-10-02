import { createFileRoute, Navigate } from '@tanstack/react-router';

export const Route = createFileRoute('/feeds/$feedId_/edit')({ component: LegacyFeedEdit });

function LegacyFeedEdit() {
  return <Navigate to="/community/$feedId/edit" params={Route.useParams()} replace />;
}

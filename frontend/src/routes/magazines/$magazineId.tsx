import { createFileRoute } from '@tanstack/react-router';
import { MagazineDetailPage } from '@/pages/MagazineDetailPage';

export const Route = createFileRoute('/magazines/$magazineId')({
  component: MagazineDetailRoute,
});

function MagazineDetailRoute() {
  const { magazineId } = Route.useParams();
  return <MagazineDetailPage magazineId={Number(magazineId)} />;
}

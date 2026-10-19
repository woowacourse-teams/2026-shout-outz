import { createFileRoute } from '@tanstack/react-router';
import { MagazinesPage } from '@/pages/MagazinesPage';

export const Route = createFileRoute('/magazines/')({
  component: MagazinesPage,
});

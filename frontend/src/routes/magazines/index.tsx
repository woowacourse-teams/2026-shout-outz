import { createFileRoute } from '@tanstack/react-router';
import { MagazinesPage } from '@/pages/MagazinesPage';

export const Route = createFileRoute('/magazines/')({
  // 미리 만들지 않는 목록 화면이지만 검색에 노출하도록 sitemap.xml에 넣는다.
  staticData: { sitemap: true },
  component: MagazinesPage,
});

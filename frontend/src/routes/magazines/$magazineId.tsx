import { createFileRoute } from '@tanstack/react-router';
import { fetchMagazines } from '@/apis/magazine';
import { MagazineDetailPage } from '@/pages/MagazineDetailPage';

export const Route = createFileRoute('/magazines/$magazineId')({
  // 임시 SSG: 콘텐츠 변경은 재빌드로 반영합니다. 정식 갱신 정책과 조회수 미집계 조회는 후속 과제입니다.
  staticData: {
    prerender: true,
    generateStaticParams: async () => {
      const params: Array<{ magazineId: string }> = [];
      const cursors = new Set<string>();
      let cursor: string | undefined;
      for (;;) {
        const page = await fetchMagazines({ cursor, size: 50 });
        params.push(...page.data.map(({ id }) => ({ magazineId: String(id) })));
        const next = page.meta.nextCursor;
        if (!page.meta.hasNext || !next) break;
        if (cursors.has(next)) throw new Error('매거진 목록의 다음 커서가 반복됩니다.');
        cursors.add(next);
        cursor = next;
      }
      return params;
    },
  },
  component: MagazineDetailRoute,
});

function MagazineDetailRoute() {
  const { magazineId } = Route.useParams();
  return <MagazineDetailPage magazineId={Number(magazineId)} />;
}

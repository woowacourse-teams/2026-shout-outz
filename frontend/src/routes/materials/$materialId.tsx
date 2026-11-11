import { createFileRoute, notFound } from '@tanstack/react-router';

import { MaterialPage } from '@/pages/MaterialPage';

// 탐색 화면과 같이 CSR로만 둔다(prerender·ssr을 지정하지 않는다).
export const Route = createFileRoute('/materials/$materialId')({
  beforeLoad: ({ params }) => {
    // 서버 자료 ID(resourceId)는 1 이상의 정수다.
    if (!/^[1-9]\d*$/.test(params.materialId)) throw notFound();
  },
  component: MaterialRoute,
});

function MaterialRoute() {
  const { materialId } = Route.useParams();
  return <MaterialPage resourceId={Number(materialId)} />;
}

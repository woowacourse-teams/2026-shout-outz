import { createFileRoute } from '@tanstack/react-router';

import { MaterialPage } from '@/pages/MaterialPage';

// 탐색 화면과 같이 CSR로만 둔다(prerender·ssr을 지정하지 않는다).
export const Route = createFileRoute('/materials/$materialId')({
  component: MaterialRoute,
});

function MaterialRoute() {
  const { materialId } = Route.useParams();
  return <MaterialPage materialId={materialId} />;
}

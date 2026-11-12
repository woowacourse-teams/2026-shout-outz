import { createFileRoute, Navigate } from '@tanstack/react-router';

import {
  PROJECT_TAB_PATHS,
  ProjectDetailRoute,
  projectDetailStaticData,
} from '@/routes/projects/-project-detail';

interface ProjectDetailSearch {
  /** 탭을 주소 뒤에 붙이던 예전 링크(?tab=material). 들어오면 그 탭 주소로 보낸다. */
  tab?: 'material' | 'intro';
}

// 경험 탭. 자료·소개 탭은 $slug_.materials.tsx, $slug_.intro.tsx다.
export const Route = createFileRoute('/projects/$slug')({
  validateSearch: (search: Record<string, unknown>): ProjectDetailSearch =>
    search.tab === 'material' || search.tab === 'intro' ? { tab: search.tab } : {},
  staticData: projectDetailStaticData,
  component: RouteComponent,
});

function RouteComponent() {
  const { slug } = Route.useParams();
  const { tab } = Route.useSearch();

  if (tab) return <Navigate to={PROJECT_TAB_PATHS[tab]} params={{ slug }} replace />;
  return <ProjectDetailRoute slug={slug} tab="experience" />;
}

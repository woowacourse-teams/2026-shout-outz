import { createFileRoute } from '@tanstack/react-router';

import { ProjectDetailRoute, projectDetailStaticData } from '@/routes/projects/-project-detail';

// 프로젝트 상세 소개 탭
export const Route = createFileRoute('/projects/$slug_/intro')({
  staticData: projectDetailStaticData,
  component: RouteComponent,
});

function RouteComponent() {
  const { slug } = Route.useParams();
  return <ProjectDetailRoute slug={slug} tab="intro" />;
}

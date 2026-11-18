import { createFileRoute } from '@tanstack/react-router';

import { ProjectDetailRoute, projectDetailStaticData } from '@/routes/projects/-project-detail';

// 프로젝트 상세 자료 탭
export const Route = createFileRoute('/projects/$slug_/materials')({
  staticData: projectDetailStaticData,
  // 탐색에서 기술 주제로 거른 프로젝트를 열면 그 주제 이름이 온다. 왼쪽 사이드바에서 먼저 고른다.
  validateSearch: (search: Record<string, unknown>): { topic?: string } => {
    const topic = typeof search.topic === 'string' ? search.topic.trim() : '';
    return topic ? { topic } : {};
  },
  component: RouteComponent,
});

function RouteComponent() {
  const { slug } = Route.useParams();
  const { topic } = Route.useSearch();
  return <ProjectDetailRoute slug={slug} tab="material" materialTopic={topic} />;
}

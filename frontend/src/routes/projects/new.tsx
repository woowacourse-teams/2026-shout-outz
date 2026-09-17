import { Suspense } from 'react';
import { createFileRoute } from '@tanstack/react-router';

import { ProjectCreatePage } from '@/pages/ProjectCreatePage';

export const Route = createFileRoute('/projects/new')({
  component: RouteComponent,
  errorComponent: ProjectCreateError,
});

function RouteComponent() {
  return (
    <Suspense fallback={<ProjectCreateMessage>등록 화면을 불러오는 중…</ProjectCreateMessage>}>
      <ProjectCreatePage />
    </Suspense>
  );
}

function ProjectCreateError() {
  return <ProjectCreateMessage>등록 화면을 불러오지 못했습니다.</ProjectCreateMessage>;
}

function ProjectCreateMessage({ children }: { children: string }) {
  return (
    <main className="px-4 pt-5 pb-7 md:px-16 md:pt-10 md:pb-20">
      <p className="text-sm text-gray-600">{children}</p>
    </main>
  );
}

import { Suspense } from 'react';
import { createFileRoute } from '@tanstack/react-router';
import { ProjectEditPage } from '@/pages/ProjectEditPage';

export const Route = createFileRoute('/projects/$id_/edit')({ component: ProjectEditRoute });

function ProjectEditRoute() {
  const { id } = Route.useParams();
  const navigate = Route.useNavigate();

  return (
    <Suspense
      fallback={<p className="px-4 py-12 text-center text-gray-600">수정 화면을 불러오는 중…</p>}
    >
      <ProjectEditPage
        projectId={id}
        onSaved={(approvalStatus) => {
          if (approvalStatus === 'APPROVED') {
            void navigate({ to: '/projects/$id', params: { id } });
          } else {
            void navigate({ to: '/projects' });
          }
        }}
      />
    </Suspense>
  );
}

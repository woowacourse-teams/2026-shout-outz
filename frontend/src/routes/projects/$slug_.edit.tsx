import { Suspense } from 'react';
import { createFileRoute } from '@tanstack/react-router';
import { ProjectEditPage } from '@/pages/ProjectEditPage';
import { toProjectSlug, toProjectSlugParam } from '@/utils/project';

export const Route = createFileRoute('/projects/$slug_/edit')({ component: ProjectEditRoute });

function ProjectEditRoute() {
  const { slug: slugParam } = Route.useParams();
  const slug = toProjectSlug(slugParam);
  const navigate = Route.useNavigate();

  return (
    <Suspense
      fallback={<p className="px-4 py-12 text-center text-gray-600">수정 화면을 불러오는 중…</p>}
    >
      <ProjectEditPage
        slug={slug}
        onSaved={(approvalStatus) => {
          if (approvalStatus === 'APPROVED') {
            void navigate({ to: '/projects/$slug', params: { slug: toProjectSlugParam(slug) } });
          } else {
            void navigate({ to: '/projects' });
          }
        }}
      />
    </Suspense>
  );
}

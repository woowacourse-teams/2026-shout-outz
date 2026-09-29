import { useSuspenseQuery } from '@tanstack/react-query';
import { projectDetailQueryOptions } from '@/api/project-detail';
import { sessionQuery } from '@/apis/session';
import { ProjectForm } from '@/pages/ProjectCreatePage';

export function ProjectEditPage({
  slug,
  onSaved,
}: {
  slug: string;
  onSaved: (approvalStatus: 'APPROVED' | 'PENDING' | 'REJECTED') => void;
}) {
  const { data: session } = useSuspenseQuery(sessionQuery);

  if (session.status !== 'AUTHENTICATED' || session.userId == null) {
    return <p className="px-4 py-12 text-center text-gray-600">로그인 후 수정할 수 있습니다.</p>;
  }

  return (
    <AuthenticatedProjectEdit slug={slug} userId={session.userId} onSaved={onSaved} />
  );
}

function AuthenticatedProjectEdit({
  slug,
  userId,
  onSaved,
}: {
  slug: string;
  userId: number;
  onSaved: (approvalStatus: 'APPROVED' | 'PENDING' | 'REJECTED') => void;
}) {
  const { data: project } = useSuspenseQuery({
    ...projectDetailQueryOptions(slug),
    queryKey: ['project-detail', slug, String(userId)],
    staleTime: 0,
  });

  if (!project.editable) {
    return <p className="px-4 py-12 text-center text-gray-600">수정 권한이 없습니다.</p>;
  }

  return (
    <ProjectForm key={project.slug} userId={userId} initialProject={project} onSaved={onSaved} />
  );
}

import { useSuspenseQuery } from '@tanstack/react-query';
import { projectListQueryOptions } from '@/api/project-list';
import { ProjectCard } from '@/components/projects/ProjectCard';

export function ProjectList() {
  const { data: projects } = useSuspenseQuery(projectListQueryOptions());

  if (projects.length === 0) {
    return <p className="py-16 text-center text-gray-500">등록된 프로젝트가 없습니다.</p>;
  }

  return (
    <ul
      aria-label="프로젝트 목록"
      className="grid grid-cols-1 gap-x-5 gap-y-8 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4"
    >
      {projects.map((project) => (
        <li key={project.id} className="min-w-0">
          <ProjectCard project={project} />
        </li>
      ))}
    </ul>
  );
}

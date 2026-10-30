import { useSuspenseInfiniteQuery } from '@tanstack/react-query';

import { projectListInfiniteQueryOptions } from '@/api/project-list';
import { ProjectCardLink } from '@/components/projects/ProjectCardLink';
import { DEFAULT_PROJECT_FILTER } from '@/constants/project';

export interface LatestProjectListProps {
  limit: number;
  surface: string;
}

/** 최신 프로젝트 몇 개를 보여준다. `/projects` 첫 페이지와 같은 쿼리라 캐시를 함께 쓴다. */
export function LatestProjectList({ limit, surface }: LatestProjectListProps) {
  const query = useSuspenseInfiniteQuery(projectListInfiniteQueryOptions(DEFAULT_PROJECT_FILTER));
  const projects = query.data.pages[0]?.projects.slice(0, limit) ?? [];

  if (projects.length === 0) {
    return <p className="py-16 text-center text-gray-500">등록된 프로젝트가 없습니다.</p>;
  }

  return (
    <ul aria-label="최신 프로젝트" className="grid grid-cols-1 gap-6 md:grid-cols-3">
      {projects.map((project) => (
        <li key={project.slug} className="min-w-0">
          <ProjectCardLink project={project} surface={surface} />
        </li>
      ))}
    </ul>
  );
}

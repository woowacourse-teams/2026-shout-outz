import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { IconSearch } from '@tabler/icons-react';

import { projectListInfiniteQueryOptions } from '@/api/project-list';
import { Button } from '@/components/Button';
import { ProjectCard } from '@/components/projects/ProjectCard';
import { countProjectFilters } from '@/constants/project';
import { type ProjectFilter } from '@/types/project';

export interface ProjectListProps {
  filter: ProjectFilter;
  onResetFilter: () => void;
}

export function ProjectList({ filter, onResetFilter }: ProjectListProps) {
  const query = useSuspenseInfiniteQuery(projectListInfiniteQueryOptions(filter));
  const projects = query.data.pages.flatMap((page) => page.projects);

  if (projects.length === 0) {
    return <EmptyResult filter={filter} onResetFilter={onResetFilter} />;
  }

  return (
    <>
      <ul
        aria-label="프로젝트 목록"
        className="grid grid-cols-1 gap-x-5 gap-y-8 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4"
      >
        {projects.map((project) => (
          <li key={project.id} className="min-w-0">
            <Link
              to="/projects/$id"
              params={{ id: String(project.id) }}
              className="focus-visible:outline-primary-600 block rounded-xl focus-visible:outline-2"
            >
              <ProjectCard
                title={project.title}
                tagline={project.tagline}
                cohort={project.cohort}
                likeCount={project.likeCount}
                commentCount={project.commentCount}
                techTags={project.techTags}
                thumbnailUrl={project.thumbnailUrl}
                members={project.members}
              />
            </Link>
          </li>
        ))}
      </ul>
      {query.hasNextPage && (
        <div className="mt-8 grid">
          <Button
            variant="outline"
            disabled={query.isFetchingNextPage}
            onClick={() => void query.fetchNextPage({ cancelRefetch: false })}
          >
            {query.isFetchingNextPage ? '불러오는 중…' : '프로젝트 더 보기'}
          </Button>
        </div>
      )}
    </>
  );
}

function EmptyResult({ filter, onResetFilter }: ProjectListProps) {
  const filtered = countProjectFilters(filter) > 0;
  const keyword = filter.keyword.trim();

  if (!filtered) {
    return <p className="py-16 text-center text-gray-500">등록된 프로젝트가 없습니다.</p>;
  }

  return (
    <div className="flex flex-col items-center gap-3 py-16 text-center">
      <IconSearch className="size-8 text-gray-400" aria-hidden="true" />
      <p className="font-bold text-gray-900">검색 결과가 없습니다.</p>
      <p className="max-w-md text-sm leading-relaxed text-gray-500">
        {keyword
          ? `'${keyword}'에 일치하는 프로젝트를 찾을 수 없습니다. 다른 검색어를 입력하거나 필터를 초기화해보세요.`
          : '고른 조건에 맞는 프로젝트를 찾을 수 없습니다. 필터를 초기화해보세요.'}
      </p>
      <Button variant="outline" className="mt-1" onClick={onResetFilter}>
        검색 및 필터 초기화
      </Button>
    </div>
  );
}

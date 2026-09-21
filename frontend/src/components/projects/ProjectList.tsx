import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { projectListInfiniteQueryOptions } from '@/api/project-list';
import { ProjectCard } from '@/components/projects/ProjectCard';
import { Button } from '@/components/Button';

export function ProjectList() {
  const query = useSuspenseInfiniteQuery(projectListInfiniteQueryOptions());
  const projects = query.data.pages.flatMap((page) => page.projects);

  if (projects.length === 0) {
    return <p className="py-16 text-center text-gray-500">등록된 프로젝트가 없습니다.</p>;
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

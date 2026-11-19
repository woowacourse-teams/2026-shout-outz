import { useQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';

import { projectDetailQueryOptions } from '@/api/project-detail';
import type { MatchedProject } from '@/apis/explore';
import { ExploreMoreButton } from '@/components/explore/ExploreMoreButton';
import { ExploreProjectCard } from '@/components/project-explore/ExploreProjectCard';
import { toProjectSlugParam } from '@/utils/project';

export function MatchedProjects({
  projects,
  onMore,
}: {
  projects: MatchedProject[];
  onMore?: () => void;
}) {
  return (
    <section aria-labelledby="matched-projects" className="mb-12">
      <h2 id="matched-projects" className="text-sm font-bold text-gray-700">
        프로젝트
      </h2>
      <div className="mt-5 grid grid-cols-1 gap-x-5 gap-y-8 sm:grid-cols-2 lg:grid-cols-3">
        {projects.map((project) => (
          <MatchedProjectCard key={project.projectSlug} project={project} />
        ))}
      </div>
      {onMore && (
        <ExploreMoreButton icon="right" onClick={onMore}>
          프로젝트 모두 보기
        </ExploreMoreButton>
      )}
    </section>
  );
}

function MatchedProjectCard({ project }: { project: MatchedProject }) {
  const detail = useQuery({ ...projectDetailQueryOptions(project.projectSlug), retry: false });
  if (detail.isPending) {
    return (
      <div
        role="status"
        aria-label={`${project.name} 프로젝트 불러오는 중`}
        className="animate-pulse motion-reduce:animate-none"
      >
        <div className="aspect-[8/5] rounded-xl bg-gray-100" />
        <div className="mt-3 h-3 w-20 rounded bg-gray-100" />
        <div className="mt-2 h-5 w-2/3 rounded bg-gray-100" />
        <div className="mt-2 h-4 w-full rounded bg-gray-100" />
      </div>
    );
  }
  if (!detail.data) {
    return (
      <Link
        to="/projects/$slug"
        params={{ slug: toProjectSlugParam(project.projectSlug) }}
        className="font-bold underline"
      >
        {project.name}
      </Link>
    );
  }
  return (
    <ExploreProjectCard
      slug={detail.data.slug}
      title={detail.data.title}
      tagline={detail.data.tagline}
      cohort={detail.data.cohort}
      likeCount={detail.data.likeCount}
      thumbnailUrl={detail.data.imageUrl}
    />
  );
}

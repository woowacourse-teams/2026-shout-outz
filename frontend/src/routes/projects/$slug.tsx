import { createFileRoute } from '@tanstack/react-router';
import { fetchProjectList } from '@/api/project-list';
import type { ProjectDetailTab } from '@/components/projects/ProjectDetailContent';
import { ProjectDetailPage } from '@/pages/ProjectDetailPage';
import { toProjectSlug, toProjectSlugParam } from '@/utils/project';

interface ProjectDetailSearch {
  /** 비어 있으면 경험 탭 */
  tab?: Exclude<ProjectDetailTab, 'experience'>;
}

export const Route = createFileRoute('/projects/$slug')({
  validateSearch: (search: Record<string, unknown>): ProjectDetailSearch =>
    search.tab === 'material' || search.tab === 'intro' ? { tab: search.tab } : {},
  component: RouteComponent,
  staticData: {
    prerender: true,
    generateStaticParams: async () => {
      const projects = await fetchProjectList();
      return projects.map((project) => ({ slug: toProjectSlugParam(project.slug) }));
    },
  },
});

function RouteComponent() {
  const { slug } = Route.useParams();
  const { tab } = Route.useSearch();
  const navigate = Route.useNavigate();

  return (
    <ProjectDetailPage
      slug={toProjectSlug(slug)}
      // 프로토타입 탭 순서대로 처음에는 경험 탭을 보여 준다.
      tab={tab ?? 'experience'}
      onTabChange={(next) =>
        void navigate({
          search: next === 'experience' ? {} : { tab: next },
          replace: true,
          resetScroll: false,
        })
      }
    />
  );
}

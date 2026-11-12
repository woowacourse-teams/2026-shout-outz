import { useNavigate, type StaticDataRouteOption } from '@tanstack/react-router';

import { fetchProjectList } from '@/api/project-list';
import type { ProjectDetailTab } from '@/components/projects/ProjectDetailContent';
import { ProjectDetailPage } from '@/pages/ProjectDetailPage';
import { toProjectSlug, toProjectSlugParam } from '@/utils/project';
import { analytics } from '@/utils/analytics';

/**
 * 프로젝트 상세 경험·자료·소개는 탭마다 주소가 따로 있어, 빌드할 때 프로젝트마다 세 페이지를 미리 만든다(SSG).
 * 같은 프로젝트 목록으로 세 라우트의 경로를 펼친다.
 */
export const projectDetailStaticData = {
  prerender: true,
  generateStaticParams: async () => {
    const projects = await fetchProjectList();
    return projects.map((project) => ({ slug: toProjectSlugParam(project.slug) }));
  },
} satisfies StaticDataRouteOption;

export const PROJECT_TAB_PATHS = {
  experience: '/projects/$slug',
  material: '/projects/$slug/materials',
  intro: '/projects/$slug/intro',
} as const satisfies Record<ProjectDetailTab, string>;

/** 세 탭 라우트가 함께 쓰는 라우팅 컴포넌트. 탭을 누르면 그 탭의 주소로 바꾼다. */
export function ProjectDetailRoute({ slug, tab }: { slug: string; tab: ProjectDetailTab }) {
  const navigate = useNavigate();

  return (
    <ProjectDetailPage
      slug={toProjectSlug(slug)}
      tab={tab}
      onTabChange={(next) => {
        analytics.track({
          name: 'project_detail_tab_changed',
          slug: toProjectSlug(slug),
          tab: next,
        });
        void navigate({
          to: PROJECT_TAB_PATHS[next],
          params: { slug },
          replace: true,
          resetScroll: false,
        });
      }}
    />
  );
}

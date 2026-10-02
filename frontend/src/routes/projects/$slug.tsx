import { createFileRoute } from '@tanstack/react-router';
import { fetchProjectList } from '@/api/project-list';
import { ProjectDetailPage } from '@/pages/ProjectDetailPage';
import { toProjectSlug, toProjectSlugParam } from '@/utils/project';

export const Route = createFileRoute('/projects/$slug')({
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
  return <ProjectDetailPage slug={toProjectSlug(slug)} />;
}

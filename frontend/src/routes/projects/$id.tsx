import { createFileRoute } from '@tanstack/react-router';
import { fetchProjectList } from '@/api/project-list';
import { ProjectDetailPage } from '@/pages/ProjectDetailPage';

export const Route = createFileRoute('/projects/$id')({
  component: RouteComponent,
  staticData: {
    prerender: true,
    generateStaticParams: async () => {
      const projects = await fetchProjectList();
      return projects.map((project) => ({ id: String(project.id) }));
    },
  },
});

function RouteComponent() {
  const { id } = Route.useParams();
  return <ProjectDetailPage projectId={id} />;
}

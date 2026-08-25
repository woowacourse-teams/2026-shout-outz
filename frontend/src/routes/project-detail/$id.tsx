import { Suspense } from 'react';
import { createFileRoute } from '@tanstack/react-router';
import { useSuspenseQuery } from '@tanstack/react-query';
import { fetchProjects, projectQueryOptions } from '../../api/project';
import { ProjectDetailPage } from '../../components/project-detail/ProjectDetailPage';
import { ProjectDetailError } from '../../components/project-detail/ProjectDetailError';

// 페이지 단위 Suspense 적용은 createFileRoute의 component에서 처리합니다.
export const Route = createFileRoute('/project-detail/$id')({
    component: RouteComponent,
    errorComponent: ProjectDetailError,
    staticData: {
        prerender: true,
        generateStaticParams: async () => {
            const projects = await fetchProjects();
            return projects.map((project) => ({ id: project.id }));
        },
    },
});

function RouteComponent() {
    return (
        <Suspense
            fallback={<p className="mx-auto max-w-3xl px-4 py-10 text-gray-400">프로젝트 정보를 불러오는 중...</p>}
        >
            <ProjectDetailLoader />
        </Suspense>
    );
}

// useSuspenseQuery는 위 Suspense 경계 "안"에서 호출돼야 fallback이 정상 동작하므로, params →
// 데이터 준비까지가 이 컴포넌트의 역할입니다. 실제 출력은 project를 props로 받는
// ProjectDetailPage(components/project-detail)에 위임합니다.
function ProjectDetailLoader() {
    const { id } = Route.useParams();
    const { data: project } = useSuspenseQuery(projectQueryOptions(id));

    return <ProjectDetailPage project={project} />;
}

import { Suspense } from 'react';
import { createFileRoute } from '@tanstack/react-router';
import { useSuspenseQuery } from '@tanstack/react-query';
import { projectsQueryOptions } from '../../api/project';
import { ProjectListView } from '../../components/project-detail/ProjectListView';

// 페이지 단위 Suspense 적용은 createFileRoute의 component에서 처리합니다.
export const Route = createFileRoute('/project-detail/')({
    component: RouteComponent,
    staticData: {
        prerender: true,
    },
});

function RouteComponent() {
    return (
        <Suspense fallback={<p className="mx-auto max-w-3xl px-4 py-10 text-gray-400">프로젝트 목록을 불러오는 중...</p>}>
            <ProjectListLoader />
        </Suspense>
    );
}

// 페이지 진입에 필요한 데이터는 Route에서 준비하고, 결과는 렌더링할 컴포넌트에 props로 전달합니다.
function ProjectListLoader() {
    const { data: projects } = useSuspenseQuery(projectsQueryOptions());

    return <ProjectListView projects={projects} />;
}

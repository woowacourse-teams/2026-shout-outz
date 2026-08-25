import type { Project } from '../../api/project';
import { ProjectDetailView } from './ProjectDetailView';
import { MakerSection } from './MakerSection';

// 라우트 파일(routes/project-detail/$id.tsx)은 params/쿼리 같은 라우팅 관심사까지만 다루고,
// 실제로 project를 props로 받아 출력하는 건 이 컴포넌트가 맡습니다. title/meta는 TanStack
// Router의 head API 대신 여기서 직접 선언합니다 - React 19가 실제 document의 <head>로 옮겨줍니다.
export function ProjectDetailPage({ project }: { project: Project }) {
    return (
        <>
            <title>{`${project.name} | Drop-it`}</title>
            <meta name="description" content={project.description} />
            <div className="mx-auto max-w-3xl px-4 py-10">
                <ProjectDetailView project={project} />
                <MakerSection projectId={project.id} />
            </div>
        </>
    );
}

import { useSuspenseQuery } from '@tanstack/react-query';
import { makerQueryOptions } from '../../api/project';

// 상세 페이지 전역이 아니라 이 컴포넌트에서만 필요한 제작자 정보를 여기서 직접 불러옵니다.
export function MakerInfo({ projectId }: { projectId: string }) {
    const { data: maker } = useSuspenseQuery(makerQueryOptions(projectId));

    return (
        <aside className="mt-8 border-t pt-6">
            <h2 className="text-lg font-semibold">제작자</h2>
            <p className="mt-1 font-medium">{maker.name}</p>
            <p className="text-gray-600">{maker.bio}</p>
        </aside>
    );
}
